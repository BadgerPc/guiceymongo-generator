/**
 *      Copyright (C) 2010 Lowereast Software
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.mattinsler.guiceymongo.data.generator.parser;

import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import com.mattinsler.guiceymongo.data.generator.TypeRegistry;
import com.mattinsler.guiceymongo.data.generator.option.Option;
import com.mattinsler.guiceymongo.data.generator.property.BlobProperty;
import com.mattinsler.guiceymongo.data.generator.property.ListProperty;
import com.mattinsler.guiceymongo.data.generator.property.MapProperty;
import com.mattinsler.guiceymongo.data.generator.property.PrimitiveProperty;
import com.mattinsler.guiceymongo.data.generator.property.Property;
import com.mattinsler.guiceymongo.data.generator.property.SetProperty;
import com.mattinsler.guiceymongo.data.generator.property.UserEnumProperty;
import com.mattinsler.guiceymongo.data.generator.property.UserDataProperty;
import com.mattinsler.guiceymongo.data.generator.type.BlobType;
import com.mattinsler.guiceymongo.data.generator.type.ListType;
import com.mattinsler.guiceymongo.data.generator.type.MapType;
import com.mattinsler.guiceymongo.data.generator.type.PrimitiveType;
import com.mattinsler.guiceymongo.data.generator.type.SetType;
import com.mattinsler.guiceymongo.data.generator.type.Type;
import com.mattinsler.guiceymongo.data.generator.type.UserEnumType;
import com.mattinsler.guiceymongo.data.generator.type.UserDataType;

public class TypeParser {
	private static final String OPTION_IDENTITY = "identity";
	
	private final boolean _useCamelCaseKeys;
	private final TypeRegistry _typeRegistry;
	private final boolean _isQuiet;

	public TypeParser(TypeRegistry typeRegistry, boolean useCamelCaseKeys, boolean isQuiet) {
		_useCamelCaseKeys = useCamelCaseKeys;
		_typeRegistry = typeRegistry;
		_isQuiet = isQuiet;
	}
	
	private Object parseLiteral(Tree tree) {
		try {
			String text = tree.getText();
			if (text.startsWith("'") && text.endsWith("'"))
				return text.substring(1, text.length() - 1);
			if (text.contains("."))
				return Float.parseFloat(text);
			return Integer.parseInt(text);
		} catch (Exception e) {
			throw new RuntimeException("Could not parse literal", e);
		}
	}
	
	private Option parseOptionTree(CommonTree tree) {
		assert GuiceyDataParser.OPTION == tree.getToken().getType();
		
		List<CommonTree> children = tree.getChildren();
		Option option = new Option(children.get(0).getText());
		if (children.size() == 2 && GuiceyDataParser.PAIR != children.get(1).getToken().getType())
			option.addParameter("value", parseLiteral(children.get(1)));
		else {
			for (int x = 1; x < children.size(); ++x) {
				if (GuiceyDataParser.PAIR == children.get(x).getToken().getType() && children.get(x).getChildCount() == 2)
					option.addParameter(children.get(x).getChild(0).getText(), parseLiteral(children.get(x).getChild(1)));
			}
		}
		return option;
	}
	
	private String parseCommentTree(CommonTree tree) {
		assert GuiceyDataParser.COMMENT == tree.getToken().getType();
		
		List<CommonTree> children = tree.getChildren();
		StringBuilder commentBuilder = new StringBuilder();
		for (CommonTree child : children) {
			commentBuilder.append(child.getText()).append(' ');
		}
		return commentBuilder.toString();
	}

	private Type parseType(UserDataType scopingType, List<CommonTree> typeArguments) {
		switch (typeArguments.remove(0).getToken().getType()) {
		case GuiceyDataParser.TYPE_PRIMITIVE:
			String typeName = typeArguments.remove(0).getText();
			Type type = _typeRegistry.getScopedGuiceyType(scopingType, typeName);
			if (type == null)
				throw new RuntimeException("Could not find type " + typeName);
			return type;
		case GuiceyDataParser.TYPE_LIST:
			return new ListType(parseType(scopingType, typeArguments));
		case GuiceyDataParser.TYPE_SET:
			return new SetType(parseType(scopingType, typeArguments));
		case GuiceyDataParser.TYPE_MAP:
			Type keyType = parseType(scopingType, typeArguments);
			Type valueType = parseType(scopingType, typeArguments);
			return new MapType(keyType, valueType);
		}
		throw new RuntimeException("The parser might have messed up or there's a type here I didn't account for (which is my mess up)");
	}
	
	private void parsePropertyTree(CommonTree tree, UserDataType type) {
		assert GuiceyDataParser.PROPERTY == tree.getToken().getType();
		
		List<CommonTree> children = tree.getChildren();
		String propertyName = children.get(0).getText();
		
		children = children.subList(1, children.size());
		Type propertyType = parseType(type, children);
		
		List<Option> options = new LinkedList<Option>();
		String comment = null;
		
		for (CommonTree child : children) {
			switch (child.getToken().getType()) {
				case GuiceyDataParser.COMMENT:
					comment = parseCommentTree(child);
					break;
				case GuiceyDataParser.OPTION:
					options.add(parseOptionTree(child));
					break;
			}
		}
		
		Property<?> property;
		if (propertyType instanceof PrimitiveType) {
			property = new PrimitiveProperty(type, propertyName, (PrimitiveType)propertyType, comment, _useCamelCaseKeys);
		} else if (propertyType instanceof BlobType) {
			property = new BlobProperty(type, propertyName, (BlobType)propertyType, comment, _useCamelCaseKeys);
		} else if (propertyType instanceof UserEnumType) {
			property = new UserEnumProperty(type, propertyName, (UserEnumType)propertyType, comment, _useCamelCaseKeys);
		} else if (propertyType instanceof UserDataType) {
			property = new UserDataProperty(type, propertyName, (UserDataType)propertyType, comment, _useCamelCaseKeys);
		} else if (propertyType instanceof ListType) {
			property = new ListProperty(type, propertyName, (ListType)propertyType, comment, _useCamelCaseKeys);
		} else if (propertyType instanceof SetType) {
			property = new SetProperty(type, propertyName, (SetType)propertyType, comment, _useCamelCaseKeys);
		} else if (propertyType instanceof MapType) {
			property = new MapProperty(type, propertyName, (MapType)propertyType, comment, _useCamelCaseKeys);
		} else {
			throw new RuntimeException("Shouldn't happen");
		}

		type.addProperty(property);
		
		for (Option option : options) {
			if (option.getName().equals(TypeParser.OPTION_IDENTITY)) {
				// TODO validation...?
				type.setIdentityProperty(property);
			}
		}
	}
	
	private void parseDataTree(CommonTree tree, UserDataType parentType) {
		assert GuiceyDataParser.DATA == tree.getToken().getType();
		
		List<CommonTree> children = tree.getChildren();
		UserDataType type = _typeRegistry.getGuiceyType((parentType == null ? "" : parentType.getGuiceyType() + ".") + children.remove(0).getText());
		
		for (CommonTree child : children) {
			switch (child.getToken().getType()) {
				case GuiceyDataParser.DATA:
					parseDataTree(child, type);
					break;
				case GuiceyDataParser.ENUM:
					parseEnumTree(child, type);
					break;
				case GuiceyDataParser.PROPERTY:
					parsePropertyTree(child, type);
					break;
				case GuiceyDataParser.COMMENT:
					type.setComment(parseCommentTree(child));
					break;
			}
		}
	}
	
	private void parseEnumTree(CommonTree tree, UserDataType parentType) {
		assert GuiceyDataParser.ENUM == tree.getToken().getType();

		List<CommonTree> children = tree.getChildren();
		UserEnumType type = _typeRegistry.getGuiceyType((parentType == null ? "" : parentType.getGuiceyType() + ".") + children.remove(0).getText());
		
		for (CommonTree child : children) {
			switch (child.getToken().getType()) {
				case GuiceyDataParser.COMMENT:
					type.setComment(parseCommentTree(child));
					break;
				default:
					type.addValue(child.getText());
					break;
			}
		}
	}
	
	private void registerAllUserTypes(CommonTree tree, UserDataType parentType) {
		try {
			Type type = null;
			if (tree.getToken() != null) {
				if (GuiceyDataParser.DATA == tree.getToken().getType() && tree.getChildCount() > 0) {
					type = new UserDataType(tree.getChild(0).getText());
					if (parentType != null)
						parentType.addChildType((UserDataType)type);
				} else if (GuiceyDataParser.ENUM == tree.getToken().getType() && tree.getChildCount() > 0) {
					type = new UserEnumType(tree.getChild(0).getText());
					if (parentType != null)
						parentType.addChildType((UserEnumType)type);
				}
				if (type != null)
					_typeRegistry.addType(type);
			}
			if (tree.getChildCount() > 0) {
				for (CommonTree child : (List<CommonTree>)tree.getChildren()) {
					if (type instanceof UserDataType)
						registerAllUserTypes(child, (UserDataType)type);
					else
						registerAllUserTypes(child, null);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void parse(CommonTree tree) {
		if (tree == null)
			return;
			
		registerAllUserTypes(tree, null);
		if (!_isQuiet) {
			for (UserDataType type : _typeRegistry.getTypes(UserDataType.class))
				System.out.println(type.getJavaType());
		}
		
		for (CommonTree typeTree : (List<CommonTree>)tree.getChildren()) {
			if (GuiceyDataParser.EOF != typeTree.getToken().getType()) {
				switch (typeTree.getToken().getType()) {
					case GuiceyDataParser.DATA:
						parseDataTree(typeTree, null);
						break;
					case GuiceyDataParser.ENUM:
						parseEnumTree(typeTree, null);
						break;
				}
			}
		}
	}
}
