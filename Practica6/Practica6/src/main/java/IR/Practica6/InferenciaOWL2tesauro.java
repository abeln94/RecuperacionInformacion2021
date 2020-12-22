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
 * Aplicación de la inferencia a un caso util para la recuperaciónd e información
 * Expansion de la descripción de la colección en base a la jerarquía de un tesauro
 * Permite comparar el modelo sin y con inferencia
 */
public class InferenciaOWL2tesauro{

	public static void main (String args[]) throws Exception{
		
		// cargamos el modelo y la coleccion
		Model colec = FileManager.get().loadModel("librosColeccion.ttl","TURTLE");
		Model model = FileManager.get().loadModel("librosModelo.ttl","TURTLE");
		
		
		//mezclamos modelo y coleccion
		Model union = ModelFactory.createUnion(model, colec);
		
		//creamos un modelo de inferencia OWL2
		InfModel inf = ModelFactory.createInfModel(PelletReasonerFactory.theInstance().create(), union);
					
		//borramos elementos del modelo para facilitar la visualizacion de lo que nos interesa
		Model model2 = borrarRecursosOWL(inf);
		
		//guardamos el modelo derivado 
		model2.write(new FileOutputStream(new File("librosColeccionInf.ttl")),"TURTLE");
	}
	
	/**
	 * borramos las clases del modelo owl que se añaden automáticamene al hacer la inferencia
	 * simplemente para facilitar la visualización de la parte que nos interesa
	 * si quieres ver todo lo que genera el motor de inferencia comenta estas lineas
	 * también borramos el modelo de skos para dejar solo la coleccion y que se vea mejor
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
		
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#definition"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#broader"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#narrower"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#prefLabel"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#altLabel"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#Concept"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#Collection"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#relatedMatch"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#changeNote"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#hiddenLabel"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#related"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#note"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#ConceptScheme"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#broaderTransitive"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#narrowerTransitive"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#historyNote"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#broadMatch"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#OrderedCollection"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#inScheme"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#narrowMatch"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#notation"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#example"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#hasTopConcept"), null, (RDFNode)null);		
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#mappingRelation"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#memberList"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#semanticRelation"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#topConceptOf"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#exactMatch"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#editorialNote"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#closeMatch"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#member"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://www.w3.org/2004/02/skos/core#scopeNote"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://purl.org/dc/terms/subject"), null, (RDFNode)null);
		model2.removeAll(inf.createResource("http://purl.org/dc/terms/title"), null, (RDFNode)null);
		
		
		return model2;
	}
	
	
}
