/*
 *  Copyright 2016 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Modified for Minecraft Web: disabled. TeaVM 0.15's pass lets an exception handler reuse a field value that
 *  is first read inside the protected block, although the handler can be entered from an earlier instruction,
 *  before that read. The generated JavaScript then reads another variable: ThreadPollServers wrote a failed
 *  ping's "Can't reach server" to the multiplayer screen instead of the server entry, which stayed at
 *  "Polling.." forever. JavaScript engines remove repeated field reads themselves, so little is lost.
 */
package org.teavm.model.optimization;

import org.teavm.model.Program;

public class RepeatedFieldReadElimination implements MethodOptimization {
    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        return false;
    }
}
