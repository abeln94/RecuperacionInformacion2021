/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools;


import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.es.SpanishLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Custom {@link Analyzer}
 *
 * @since 3.1
 */
public final class CustomAnalyzer extends StopwordAnalyzerBase {
    private final boolean applySpanish;

    /**
     * Returns an unmodifiable instance of the default stop words set.
     *
     * @return default stop words set.
     */
    public static CharArraySet getDefaultStopSet() {
        try {
            return WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class,
                    "spanish_stop.txt", StandardCharsets.UTF_8));
        } catch (IOException ex) {
            // default set should always be present as it is part of the
            // distribution (JAR)
            throw new RuntimeException("Unable to load default stopword set");
        }
    }

    /**
     * Builds an analyzer
     */
    public CustomAnalyzer(boolean applySpanish) {
        super(getDefaultStopSet());
        this.applySpanish = applySpanish;
    }

    /**
     * Creates a
     * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
     * which tokenizes all the text in the provided {@link Reader}.
     *
     * @return A
     * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
     * built from an {@link StandardTokenizer} filtered with
     * {@link LowerCaseFilter}, {@link StopFilter}
     * , {@link SetKeywordMarkerFilter} if a stem exclusion set is
     * provided and {@link SpanishLightStemFilter}.
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();

        TokenStream result = new LowerCaseFilter(source);
        result = new StopFilter(result, stopwords);
        if (applySpanish)
            // result = new SpanishLightStemFilter(result);
            result = new SnowballFilter(result, "Spanish");
        result = new ASCIIFoldingFilter(result);

        return new TokenStreamComponents(source, result);
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }
}
