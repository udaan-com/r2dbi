/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.udaan.r2dbi

import com.udaan.r2dbi.utils.isIterableOrArray
import com.udaan.r2dbi.utils.iteratorFrom
import java.lang.reflect.Method

internal class SqlBatchMethodHandler(method: Method) : SqlMethodHandler(method) {
    private val batchAnnotation: SqlBatch = method.getAnnotation(SqlBatch::class.java)
    private val listMethodArgBinder = getListMethodArg()

    override fun getSqlString(): String {
        return batchAnnotation.value
    }

    override fun returnUpdatedCount(): Boolean = batchAnnotation.returnUpdatedRows

    override fun bindArgs(
        statementContext: StatementContext,
        args: Array<out Any?>
    ) {
        val listArgIterator = listMethodArgBinder?.let {
            args.getOrNull(it.getIndex())
        }?.let {
            iteratorFrom(it)
        } ?: return super.bindArgs(statementContext, args)

        for (argValue in listArgIterator) {
            args.forEachIndexed { index, value ->
                val (bindValue, methodArgBinder) = if (index == listMethodArgBinder.getIndex()) {
                    argValue to listMethodArgBinder
                } else {
                    value to getArgBinderAtIndex(index)
                }

                methodArgBinder.bindArg(statementContext, bindValue)
            }

            if (listArgIterator.hasNext()) {
                statementContext.addNewBindings()
            }
        }
    }

    private fun getListMethodArg(): BoundedArgumentBinder? {
        val list = filterArgBinders { isIterableOrArray(it.getArgType()) }
        return when (list.size) {
            0 -> null
            1 -> list.first()
            else -> throw IllegalArgumentException("Only 1 list element is allowed, has more than 1 ${list.map { it.getIndex() }}")
        }
    }
}
