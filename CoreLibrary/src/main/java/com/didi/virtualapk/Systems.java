/*
 * Copyright (C) 2017 Beijing Didi Infinity Technology and Development Co.,Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.didi.virtualapk;

import android.content.Context;


/**
 * Created by renyugang on 16/12/22.
 */

/**
 * This is God class, you should not know where it's from and any details.
 */
public class Systems {

    static Context sHostContext;

    /**
     * get a Context object anywhere you want.
     * @return a Context object
     */
    public static Context getContext() {
        return sHostContext;
    }

}
