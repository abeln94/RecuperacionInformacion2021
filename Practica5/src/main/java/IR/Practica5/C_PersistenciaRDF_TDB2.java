package IR.Practica5;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb2.TDB2Factory;

/**
 * Ejemplo de lectura y escritura de un rdf en una base de datos de triples
 * Jena TDB2. 
 * 
 */
public class C_PersistenciaRDF_TDB2 {
	
	public static void main (String args[]) throws Exception{	
		//generamos un modelo de ejemplo
		Model model = A_CreacionRDF.generarEjemplo();
		
		//creamos un tdb (triplet data base) para almacenar el modelo
		//el borrado del directorio es para que se cree de cero en cada ejecución
		String directory = "DB1" ;
		FileUtils.deleteDirectory(new File(directory));
		Dataset data = TDB2Factory.connectDataset(directory);
		
		//hacemos una transacción de escritura y confirmamos los cambios
		data.begin(ReadWrite.WRITE) ;
		data.getDefaultModel().add(model);
		data.commit();
		data.end();
		
		//hacemos una transacción de lectura y mostramos el modelo recuperado del disco
		data.begin(ReadWrite.READ) ;
		Model modelo2 = data.getDefaultModel();
		modelo2.write(System.out);
		data.end();
		
	}
	
}
