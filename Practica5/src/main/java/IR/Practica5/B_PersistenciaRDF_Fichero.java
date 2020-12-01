package IR.Practica5;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;

/**
 * Ejemplo de lectura y escritura de un rdf en un fichero de texto
 * lo muestra por pantalla en diferentes formatos
 */
public class B_PersistenciaRDF_Fichero {
	
	/**
	 * Lectura y escritura de RDF en fichero de texto
	 */
	public static void main (String args[]) throws Exception{
		
		//generamos un modelo de ejemplo
		Model model = A_CreacionRDF.generarEjemplo();
		
		//lo guardamos en un fichero rdf en formato xml
		model.write(new FileOutputStream(new File("nombre.rdf")), "RDF/XML-ABBREV");
		
		//cargamos el fichero recien guardado   
	    Model model2 =  FileManager.get().loadModel("nombre.rdf","RDF/XML-ABBREV");
	    
	    //lo mostramos por pantalla en diferentes formatos de RDF
	    System.out.println("------------------------------");
	    model2.write(System.out,"N-TRIPLE"); 
	    System.out.println("------------------------------");
	    model2.write(System.out,"TURTLE"); 
	    
	}
	
}
