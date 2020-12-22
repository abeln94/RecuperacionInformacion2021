package IR.Practica6;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.FileManager;

import openllet.jena.PelletReasonerFactory;

/**
 * Crea un modelo con una jerarquia de clases y propiedades y le aplica el modelo
 * de inferencia de OWL2.
 * Como razonador integrado con jena usamos Pellet
 * Procesa una ontología de pizzas, que usa al maximo las capacidades de OWL-DL
 * Permite comparar el modelo sin y con inferencia
 */
public class InferenciaOWL2pizza{

	public static void main (String args[]) throws Exception{
		
		// cargamos el fichero deseado
		Model model = FileManager.get().loadModel("pizza.owl");
		//lo guardamos en formato turtle, para facilitar su lectura manual
		model.write(new FileOutputStream(new File("pizza.ttl")),"TURTLE");
		
		//creamos un modelo de inferencia OWL2
		InfModel inf = ModelFactory.createInfModel(PelletReasonerFactory.theInstance().create(), model);
					
		//borramos elementos del modelo para facilitar la visualizacion de lo que nos interesa
		Model model2 = borrarRecursosOWL(inf);
		
		//guardamos el modelo derivado 
		model2.write(new FileOutputStream(new File("pizzaInf.ttl")),"TURTLE");
	}
	
	/**
	 * borramos las clases del modelo owl que se añaden automáticamene al hacer la inferencia
	 * simplemente para facilitar la visualización de la parte que nos interesa
	 * si quieres ver todo lo que genera el motor de inferencia comenta estas lineas
	 */
	private static Model borrarRecursosOWL(Model inf) {
		//hacemos una copia del modelo ya que el modelo inferido es inmutable
		Model model2 = ModelFactory.createDefaultModel();
		model2.add(inf);
		model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#topDataProperty"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#topObjectProperty"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#Thing"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#bottomObjectProperty"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#Nothing"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#bottomDataProperty"), null, (RDFNode)null);
		
		return model2;
	}
	
	
}
