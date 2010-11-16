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

package com.mattinsler.guiceymongo.data.generator.property;

import com.mattinsler.guiceymongo.data.generator.type.UserEnumType;
import com.mattinsler.guiceymongo.data.generator.type.UserDataType;

public class UserEnumProperty extends Property<UserEnumType> {
	public UserEnumProperty(UserDataType enclosingType, String name, UserEnumType type, String comment, boolean useCamelCaseKeys) {
		super(enclosingType, name, type, comment, useCamelCaseKeys);
	}
	
	public String getEnumType() {
		return super.getType().getJavaType();
	}
}
