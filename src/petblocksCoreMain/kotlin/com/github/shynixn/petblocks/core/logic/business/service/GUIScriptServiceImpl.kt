package com.github.shynixn.petblocks.core.logic.business.service

import com.github.shynixn.petblocks.api.business.annotation.Inject
import com.github.shynixn.petblocks.api.persistence.entity.ScriptResult
import com.github.shynixn.petblocks.api.business.enumeration.ScriptAction
import com.github.shynixn.petblocks.api.business.service.LoggingService
import com.github.shynixn.petblocks.api.business.service.GUIScriptService
import com.github.shynixn.petblocks.core.logic.persistence.entity.ScriptResultEntity

/**
 * Created by Shynixn 2018.
 * <p>
 * Version 1.2
 * <p>
 * MIT License
 * <p>
 * Copyright (c) 2018 by Shynixn
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
class GUIScriptServiceImpl @Inject constructor(private val logger: LoggingService) : GUIScriptService {
    /**
     * Executes the given [script].
     */
    override fun executeScript(script: String): ScriptResult {
        val scriptResult = ScriptResultEntity()

        try {
            if (script.startsWith("executing action copy-pet-skin")) {
                scriptResult.action = ScriptAction.COPY_PET_SKIN
                return scriptResult
            } else if (script.startsWith("executing action call-pet")) {
                scriptResult.action = ScriptAction.CALL_PET
                return scriptResult
            } else if (script.startsWith("close-gui")) {
                scriptResult.action = ScriptAction.CLOSE_GUI
                return scriptResult
            } else if (script.startsWith("open-page")) {
                scriptResult.action = ScriptAction.OPEN_PAGE
                scriptResult.valueContainer = script.split(" ")[1]
                return scriptResult
            }
        } catch (e: Exception) {
            logger.warn("Failed to execute script '$script'.")
        }

        return ScriptResultEntity()
    }
}