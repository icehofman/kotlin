/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain

import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

abstract class ReplaceLoopResultTransformation(override val loop: KtForExpression): ResultTransformation {

    override val commentSavingRange = PsiChildRange.singleElement(loop.unwrapIfLabeled())

    override val expressionToBeReplacedByResultCallChain: KtExpression
        get() = loop.unwrapIfLabeled()

    override fun convertLoop(resultCallChain: KtExpression, commentSavingRangeHolder: CommentSavingRangeHolder): KtExpression {
        return loop.unwrapIfLabeled().replaced(resultCallChain)
    }
}

abstract class AssignToVariableResultTransformation(
        override val loop: KtForExpression,
        protected val initialization: VariableInitialization
) : ResultTransformation {

    override val commentSavingRange = PsiChildRange(initialization.initializationStatement, loop.unwrapIfLabeled())

    override val expressionToBeReplacedByResultCallChain: KtExpression
        get() = initialization.initializer

    override fun convertLoop(resultCallChain: KtExpression, commentSavingRangeHolder: CommentSavingRangeHolder): KtExpression {
        initialization.initializer.replace(resultCallChain)

        val variable = initialization.variable
        if (variable.isVar && variable.countWriteUsages() == variable.countWriteUsages(loop)) { // change variable to 'val' if possible
            variable.valOrVarKeyword.replace(KtPsiFactory(variable).createValKeyword())
        }

        val loopUnwrapped = loop.unwrapIfLabeled()

        // move initializer to the loop if needed
        var initializationStatement = initialization.initializationStatement
        if (initializationStatement.nextStatement() != loopUnwrapped) {
            val block = loopUnwrapped.parent
            assert(block is KtBlockExpression)
            val movedInitializationStatement = block.addBefore(initializationStatement, loopUnwrapped) as KtExpression
            block.addBefore(KtPsiFactory(block).createNewLine(), loopUnwrapped)

            commentSavingRangeHolder.remove(initializationStatement)

            initializationStatement.delete()
            initializationStatement = movedInitializationStatement
        }

        loopUnwrapped.delete()

        return initializationStatement
    }

    companion object {
        fun createDelegated(delegate: ResultTransformation, initialization: VariableInitialization): AssignToVariableResultTransformation {
            return object: AssignToVariableResultTransformation(delegate.loop, initialization) {
                override val presentation: String
                    get() = delegate.presentation

                override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
                    return delegate.generateCode(chainedCallGenerator)
                }
            }
        }
    }
}

class AssignSequenceTransformationResultTransformation(
        private val sequenceTransformation: SequenceTransformation,
        initialization: VariableInitialization
) : AssignToVariableResultTransformation(sequenceTransformation.loop, initialization) {

    override val presentation: String
        get() = sequenceTransformation.presentation

    override fun buildPresentation(prevTransformationsPresentation: String?): String {
        return sequenceTransformation.buildPresentation(prevTransformationsPresentation)
    }

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return sequenceTransformation.generateCode(chainedCallGenerator)
    }
}