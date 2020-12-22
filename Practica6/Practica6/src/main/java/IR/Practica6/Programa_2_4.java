package IR.Practica6;

import openllet.jena.PelletReasonerFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Crea un modelo con una jerarquia de clases y propiedades y le aplica el modelo
 * de inferencia de OWL2.
 * Como razonador integrado con jena usamos Pellet
 * Procesa una ontología de pizzas, que usa al maximo las capacidades de OWL-DL
 * Permite comparar el modelo sin y con inferencia
 */
public class Programa_2_4 {

    public static void main(String[] args) {

        // cargamos el modelo y la coleccion
        Model colec = FileManager.get().loadModel("librosColeccion.ttl", "TURTLE");
        Model model = FileManager.get().loadModel("librosModelo.ttl", "TURTLE");

        //mezclamos modelo y coleccion
        Model union = ModelFactory.createUnion(model, colec);

        //creamos un modelo de inferencia OWL2
        InfModel inf = ModelFactory.createInfModel(PelletReasonerFactory.theInstance().create(), union);

        // ------------------------- consulta al modelo -------------------------

        //definimos la consulta (tipo query)
        String queryString = "Select ?book WHERE {" +
                "?book a <http://schema.org/Book> ." +
                "?book <http://purl.org/dc/terms/subject> <http://www.gemet.com/biosfera>" +
                "}";

        System.out.println("Modelo original:");
        performQuery(queryString, union);

        System.out.println("Modelo inferido:");
        performQuery(queryString, inf);

    }

    private static void performQuery(String queryString, Model model) {
        Query query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                System.out.println(results.nextSolution());
            }
        }
    }

}
