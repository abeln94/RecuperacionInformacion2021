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

import indexfiles.indexer.Indexer;
import tools.ArgsParser;

import java.io.*;

/**
 * Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {


    private static String indexPath = "index";
    private static String docsPath = null;
    private static boolean update = false;
    private static boolean debug = false;

    /**
     * Index all text files under a directory.
     */
    public static void main(String[] args) {

        new ArgsParser("This indexes the documents in DOCS_PATH, creating a Lucene index in INDEX_PATH that can be searched with SearchFiles")
                .addRequired("-index", "The filename of the index folder", v -> indexPath = v)
                .addRequired("-docs", "The folder name of the documents to index", v -> docsPath = v)
                .addOptional("-update", "If true, keeps existing index otherwise recreates it (default false)", v -> update = Boolean.parseBoolean(v))
                .addOptional("-d", "If true, print information about the indexing process (default false)", v -> debug = Boolean.parseBoolean(v))
                .parse(args);


        try {
            new Indexer(indexPath, docsPath, !update, debug).run();
        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }

    }

}