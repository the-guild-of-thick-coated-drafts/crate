/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.analyze;


import io.crate.action.sql.SessionContext;
import io.crate.metadata.PartitionName;
import io.crate.metadata.RelationName;
import io.crate.metadata.Schemas;
import io.crate.metadata.doc.DocTableInfo;
import io.crate.metadata.table.Operation;
import io.crate.sql.tree.AlterTableOpenClose;
import io.crate.sql.tree.Table;

import static io.crate.analyze.BlobTableAnalyzer.tableToIdent;

class AlterTableOpenCloseAnalyzer {

    private final Schemas schemas;

    AlterTableOpenCloseAnalyzer(Schemas schemas) {
        this.schemas = schemas;
    }

    public AlterTableOpenCloseAnalyzedStatement analyze(AlterTableOpenClose node, SessionContext sessionContext) {
        Table table = node.table();
        RelationName relationName;
        if (node.blob()) {
            relationName = tableToIdent(table);
        } else {
            relationName = schemas.resolveRelation(table.getName(), sessionContext.searchPath());
        }

        DocTableInfo tableInfo = schemas.getTableInfo(relationName, Operation.ALTER_OPEN_CLOSE);
        PartitionName partitionName = null;
        if (tableInfo.isPartitioned()) {
            partitionName = AlterTableAnalyzer.createPartitionName(table.partitionProperties(), tableInfo, null);
        }
        return new AlterTableOpenCloseAnalyzedStatement(tableInfo, partitionName, node.openTable());
    }
}
