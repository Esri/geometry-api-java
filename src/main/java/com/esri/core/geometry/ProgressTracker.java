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

/**
 *A callback to provide progress and cancel tracking mechanism for lengthy operation.
 */
public abstract class ProgressTracker {
	/**
	 *Periodically called by a lengthy operation to check if the caller requested to cancel.
	 *@param step The current step of the operation.
	 *@param totalExpectedSteps is the number of steps the operation is expects to complete its task.
	 *@return true, if the operation can continue. Returns False, when the operation has to terminate due to a user cancelation.
	 */
	public abstract boolean progress(int step, int totalExpectedSteps);
	
	/**
	 * Checks the tracker and throws UserCancelException if tracker is not null and progress returns false
	 * @param tracker can be null, then the method does nothing.
	 */
	public static void checkAndThrow(ProgressTracker tracker) {
		if (tracker != null && !tracker.progress(-1, -1))
			throw new UserCancelException();
	}
}
