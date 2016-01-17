/*
 Copyright 1995-2015 Esri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts Dept
 380 New York Street
 Redlands, California, USA 92373

 email: contracts@esri.com
 */

package com.esri.core.geometry;

import com.esri.core.geometry.Operator.Type;

/**
 * Calculates distance between geometries.
 */
public abstract class OperatorDistance extends Operator {

    @Override
    public Type getType() {
        return Type.Distance;
    }

    /**
     * @param  geom1
     *         the first geometry of the parameter pair to be operated on.
     *
     * @param  geom2
     *         the second geometry of the parameter pair to be operated on.
     *
     * @param  progressTracker
     *         the callback used to cancel the lengthy operation. Can be null.
     *
     * @return the distance between two geometries
     */
    public abstract double execute(Geometry geom1, Geometry geom2,
            ProgressTracker progressTracker);

    public static OperatorDistance local() {
        return (OperatorDistance) OperatorFactoryLocal.getInstance()
                .getOperator(Type.Distance);
    }

}
