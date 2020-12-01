package IR.Practica5;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.FileManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Ejercicio
 */
public class D_AccesoRDF_ejercicio {

    public static void main(String[] args) {

        // cargamos el fichero deseado
        Model model = FileManager.get().loadModel("card.rdf");

        // mostramos las posibles uris
        model.listStatements().toList().stream()
                .map(statement -> statement.getSubject().getURI())
                .filter(Objects::nonNull)
                .distinct()
                .forEach(System.out::println);

        // preguntamos al usuario
        System.out.println("Elige una uri cualesquiera");
        String search;
        try {
            search = new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            System.out.println("No quieres buscar nada? ok");
            return;
        }

        List<Property> properties = model.listStatements().toList().stream()
                .filter(statement -> search.equals(statement.getSubject().getURI()))
                .map(Statement::getPredicate)
                .distinct().collect(Collectors.toList());

        System.out.println("Ese recurso tiene las propiedades:");
        properties.forEach(System.out::println);

        System.out.println("Otros recursos que también tienen esas propiedades son:");
        model.listStatements().toList().stream()
                .filter(statement -> properties.contains(statement.getPredicate()))
                .map(statement -> statement.getSubject().getURI())
                .filter(s -> !search.equals(s))
                .distinct()
                .forEach(System.out::println);
    }
}
