package IR.Practica5;


import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.FileManager;

/**
 * Ejemplo de lectura de un modelo RDF de un fichero de texto
 * y como acceder con SPARQL a los elementos que contiene
 */
public class E_AccesoSPARQL_ejercicio {

    public static void main(String[] args) {

        // cargamos el fichero deseado
        Model model = FileManager.get().loadModel("card.rdf");

        //definimos la consulta (tipo query)
        String queryString = "Select Distinct ?result WHERE {?x ?y ?result FILTER regex(?result, \".*Berners-Lee.*\") }";

        //ejecutamos la consulta y obtenemos los resultados
        doQuery(model, queryString);

        System.out.println("----------------------------------------");

        queryString = "SELECT DISTINCT ?result WHERE {" +
                "?doc <http://purl.org/dc/elements/1.1/title> ?result." +
                "?doc <http://purl.org/dc/elements/1.1/creator> <http://www.w3.org/People/Berners-Lee/card#i>" +
                "}";
        doQuery(model, queryString);

        System.out.println("----------------------------------------");

        queryString = "construct {?x ?y ?z} where {" +
                "?x <http://purl.org/dc/elements/1.1/creator> <http://www.w3.org/People/Berners-Lee/card#i>." +
                "?x ?y ?z" +
                "}";
        QueryExecutionFactory.create(QueryFactory.create(queryString), model).execConstruct().write(System.out);

    }

    private static void doQuery(Model model, String queryString) {
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode result = soln.get("result");
                if (result.isLiteral()) {
                    System.out.println(result.toString());
                } else {
                    System.out.println(result.asResource().getURI());
                }
            }
        }

    }

}
