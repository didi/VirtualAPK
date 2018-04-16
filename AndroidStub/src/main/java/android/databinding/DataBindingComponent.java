/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.databinding;
/**
 * This interface is generated during compilation to contain getters for all used instance
 * BindingAdapters. When a BindingAdapter is an instance method, an instance of the class
 * implementing the method must be instantiated. This interface will be generated with a getter
 * for each class with the name get* where * is simple class name of the declaring BindingAdapter
 * class/interface. Name collisions will be resolved by adding a numeric suffix to the getter.
 * <p>
 * An instance of this class may also be passed into static or instance BindingAdapters as the
 * first parameter.
 * <p>
 * If you are using Dagger 2, you should extend this interface and annotate the extended interface
 * as a Component.
 */
public interface DataBindingComponent {
}