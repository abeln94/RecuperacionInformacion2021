package IR.Practica5;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD;

/**
 * Ejemplo de como construir un modelo de Jena y añadir nuevos recursos
 * mediante la clase Model
 */
public class A_CreacionRDF {

    /**
     * muestra un modelo de jena de ejemplo por pantalla
     */
    public static void main(String[] args) {
        Model model = A_CreacionRDF.generarEjemplo();
        // write the model in the standard output
        model.write(System.out);
    }

    /**
     * Genera un modelo de jena de ejemplo
     */
    public static Model generarEjemplo() {
        // definiciones
        String personURI = "http://somewhere/JohnSmith";
        String givenName = "John";
        String familyName = "Smith";
        String fullName = givenName + " " + familyName;

        // crea un modelo vacio
        Model model = ModelFactory.createDefaultModel();

        // le añade las propiedades
        Resource johnSmith = model.createResource(personURI)
                .addProperty(VCARD.FN, fullName)
                .addProperty(VCARD.N,
                        model.createResource()
                                .addProperty(VCARD.Given, givenName)
                                .addProperty(VCARD.Family, familyName)
                );

        // ejercicio
        johnSmith.addProperty(RDF.type, FOAF.Person);

        // ejercicio
        Resource persona1 = model.createResource("http://somewhere/Persona1");
        Resource persona2 = model.createResource("http://somewhere/Persona2");
        persona1.addProperty(FOAF.knows, persona2);

        // ejercicio
        // rdf:resource define un recurso para identificar una propiedad de forma global
        // rdf:nodeID define el id de un nodo para usar de forma local

        // ejercicio
        // <vcard:N rdf:nodeID=“A0”/> sirve para identificar al elemento A0 (por ejemplo un nodo en blanco) de forma local
        // <vcard:N>A0<vcard:N> es un elemento que tiene como valor A0

        // ejercicio
        model.add(persona2, FOAF.knows, persona1);


        return model;
    }


}
