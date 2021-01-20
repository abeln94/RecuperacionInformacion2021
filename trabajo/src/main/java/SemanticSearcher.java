import org.apache.jena.query.*;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import tools.ArgsParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

public class SemanticSearcher {
    private static String rdfPath;
    private static String infoNeeds;
    private static String resultsFile;


    public static void main(String[] args) throws IOException {
        // parse arguments
        new ArgsParser("Converts a recordsdc collection into a turtle file")
                .addRequired("-rdf", "the rdf file where the graph is located", 1, v -> rdfPath = v.get(0))
                .addRequired("-infoNeeds", "the file with the information necessities", 1, v -> infoNeeds = v.get(0))
                .addRequired("-output", "file to output the results", 1, v -> resultsFile = v.get(0))
                .parse(args);


        //definimos la configuración del repositorio indexado
        EntityDefinition entDef = new EntityDefinition("uri", "name", ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/", "name"));
        entDef.set("description", DCTerms.description.asNode());
        TextIndexConfig config = new TextIndexConfig(entDef);
        config.setAnalyzer(new SpanishAnalyzer());
        config.setQueryAnalyzer(new SpanishAnalyzer());
        config.setMultilingualSupport(true);

        //definimos el repositorio indexado todo en memoria
        Dataset ds1 = DatasetFactory.createGeneral();
        Directory dir = new RAMDirectory();
        Dataset ds = TextDatasetFactory.createLucene(ds1, dir, config);


        // cargamos el fichero deseado y lo almacenamos en el repositorio indexado
        RDFDataMgr.read(ds.getDefaultModel(), rdfPath);


        // load queries
        Scanner sc = new Scanner(new FileInputStream(infoNeeds));
        while (sc.hasNextLine()) {
            String id = sc.next();
            String query_string = sc.nextLine();

            query_string = "prefix ri: <http://rdf.unizar.es/recuperacion_informacion/grupo_110/modelo#> \n" + query_string;

            Query query = QueryFactory.create(query_string);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
                ResultSet results = qexec.execSelect();
                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
                    System.out.println(id + "\t" + soln.get(soln.varNames().next()));
                }
            }

        }
    }


}
