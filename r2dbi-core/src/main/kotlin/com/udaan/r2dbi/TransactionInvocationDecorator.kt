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

import com.udaan.r2dbi.sql.interfaces.SqlInvocationDecorator

internal class TransactionInvocationDecorator(private val transaction: Transaction) : SqlInvocationDecorator {
    override fun decorate(invocationHandler: SqlInvocationHandler): SqlInvocationHandler {
        return SqlInvocationHandler { scopedExecutor, target, args ->
            val txScope = scopedExecutor.inTransaction(transaction.level)
            invocationHandler.invoke(txScope, target, args)
        }
    }
}
