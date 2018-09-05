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

package io.crate.lucene;

import io.crate.expression.operator.LikeOperator;
import io.crate.expression.symbol.Literal;
import io.crate.metadata.Reference;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.elasticsearch.common.lucene.BytesRefs;
import org.elasticsearch.common.lucene.search.Queries;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.RegexpFlag;

import java.util.Locale;

class AnyNotLikeQuery extends AbstractAnyQuery {

    static String negateWildcard(String wildCard) {
        return String.format(Locale.ENGLISH, "~(%s)", wildCard);
    }

    @Override
    protected Query applyArrayReference(Reference arrayReference, Literal literal, LuceneQueryBuilder.Context context) {
        String regexString = LikeOperator.patternToRegex(BytesRefs.toString(literal.value()), LikeOperator.DEFAULT_ESCAPE, false);
        regexString = regexString.substring(1, regexString.length() - 1);
        String notLike = negateWildcard(regexString);

        return new RegexpQuery(new Term(
            arrayReference.column().fqn(),
            notLike),
            RegexpFlag.COMPLEMENT.value()
        );
    }

    @Override
    protected Query applyArrayLiteral(Reference reference, Literal arrayLiteral, LuceneQueryBuilder.Context context) {
        // col not like ANY (['a', 'b']) --> not(and(like(col, 'a'), like(col, 'b')))
        String columnName = reference.column().fqn();
        MappedFieldType fieldType = context.getFieldTypeOrNull(columnName);

        BooleanQuery.Builder andLikeQueries = new BooleanQuery.Builder();
        for (Object value : toIterable(arrayLiteral.value())) {
            andLikeQueries.add(
                LikeQuery.like(reference.valueType(), fieldType, value),
                BooleanClause.Occur.MUST);
        }
        return Queries.not(andLikeQueries.build());
    }
}
