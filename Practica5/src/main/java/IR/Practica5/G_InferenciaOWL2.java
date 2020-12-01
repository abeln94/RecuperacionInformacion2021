package IR.Practica5;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import openllet.jena.PelletReasonerFactory;

/**
 * Crea un modelo con una jerarquia de clases y propiedades y le aplica el modelo
 * de inferencia de OWL2.
 * Como razonador integrado con jena usamos Pellet
 * Compara el resultado de consultas a la jerarquiía definida con y sin inferencia
 */
public class G_InferenciaOWL2 {

	public static void main (String args[]) {
		String NS = "urn:x-hp-jena:eg/";
	
		// contruimos un modelo secillo de seres vivos y sus relaciones
		Model rdfsExample = ModelFactory.createDefaultModel();
		//Definimos una jerarquía de clases de seres vivos
		Resource animalClass = rdfsExample.createResource(NS+"Animal");
		animalClass.addProperty(RDF.type, RDFS.Class);
		Resource mamiferoClass = rdfsExample.createResource(NS+"Mamifero");
		animalClass.addProperty(RDFS.subClassOf, mamiferoClass);
		Resource humanoClass = rdfsExample.createResource(NS+"Humano");
		humanoClass.addProperty(RDFS.subClassOf, animalClass);
		
		//definimos las relaciones hijo, e hijo adoptivo
		Property hijoDe = rdfsExample.createProperty(NS, "hijoDe");
		//Property padreDe = rdfsExample.createProperty(NS, "padreDe");
		//padreDe.addProperty(OWL.inverseOf, hijoDe);
		
		rdfsExample.add(hijoDe, RDF.type, RDF.Property);
		hijoDe.addProperty(RDFS.domain, animalClass);
		hijoDe.addProperty(RDFS.range, animalClass);
		
		Property hijoAdoptDe = rdfsExample.createProperty(NS, "hijoAdoptivoDe");
		rdfsExample.add(hijoAdoptDe, RDFS.subPropertyOf, hijoDe);
		hijoAdoptDe.addProperty(RDFS.domain, humanoClass);
		hijoAdoptDe.addProperty(RDFS.range, humanoClass);
		
		//añadimos las instancias al modelo y sus relaciones
		Resource luis = rdfsExample.createResource(NS+"Luis");
		Resource juan = rdfsExample.createResource(NS+"Juan");	
		luis.addProperty(hijoAdoptDe, juan);
		
		//mostramos el modelo sin inferencia
		System.out.println("------------------------------");
		rdfsExample.write(System.out,"TURTLE"); 
		System.out.println("------------------------------");
		
		//creamos un modelo de inferencia OWL2
		InfModel inf = ModelFactory.createInfModel(PelletReasonerFactory.theInstance().create(), rdfsExample);
					
		//borramos elementos del modelo para facilitar la visualizacion de lo que nos interesa
		Model model2 = borrarRecursosOWL(inf);
		
		//mostramos el fragmento deseado del modelo inferido
		System.out.println("------------------------------");
		model2.write(System.out,"TURTLE"); 
		System.out.println("------------------------------");
	}
	
	/**
	 * borramos las clases del modelo rdfs que se añaden automáticamene al hacer la inferencia
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
