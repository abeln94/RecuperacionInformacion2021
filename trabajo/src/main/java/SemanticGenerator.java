import indexfiles.extractor.Extractor;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import tools.ArgsParser;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import static indexfiles.parser.RecordsDcParser.*;

public class SemanticGenerator {

    private static final Model m = ModelFactory.createDefaultModel();
    public static final String riUri = "http://rdf.unizar.es/recuperacion_informacion/grupo_110/modelo#";
    public static final String ricUri = "http://rdf.unizar.es/recuperacion_informacion/grupo_110/conceptos#";

    public static Property pRI(String name) {
        return m.createProperty(riUri, name);
    }

    public static Property pRIC(String name) {
        return m.createProperty(ricUri, name);
    }


    private static String rdfPath;
    private static String skosPath;
    private static String owlPath;
    private static String docsPath;

    public static void main(String[] args) throws FileNotFoundException {

        // parse arguments
        new ArgsParser("Converts a recordsdc collection into a turtle file")
                .addRequired("-rdf", "the rdf file where to generate the final graph", 1, v -> rdfPath = v.get(0))
                .addRequired("-skos", "the rdf file of the skos model", 1, v -> skosPath = v.get(0))
                .addRequired("-owl", "the rdf file of the owl model of the collection", 1, v -> owlPath = v.get(0))
                .addRequired("-docs", "the folder with the files to parse", 1, v -> docsPath = v.get(0))
                .parse(args);

        // load the model
        Model model = FileManager.get().loadModel(skosPath, "TURTLE");

        // add the other model
        model.add(FileManager.get().loadModel(owlPath, "TURTLE"));

        // parse files
        Extractor extractor = new Extractor();
        extractor.indexPath(docsPath);

        System.out.println("Indexing files");
        for (File file : extractor.getFiles()) {
            String path = file.getPath();
            path = path.substring(path.lastIndexOf("\\") + 1);


            try (FileInputStream fis = new FileInputStream(file)) {
                NodeList list;

                // parse XML dom
                org.w3c.dom.Document xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);

                // create main document
                String id = xmlDoc.getElementsByTagName(PREFIX_DC + FIELD_IDENTIFIER).item(0).getTextContent();
                Resource resource = model.createResource(id);

                // type
                Property type;
                String type_string = xmlDoc.getElementsByTagName(PREFIX_DC + FIELD_TYPE).item(0).getTextContent();
                switch (type_string.strip()) {
                    case "TAZ-PFC":
                        type = pRI("pfc");
                        break;
                    case "TAZ-TFG":
                        type = pRI("tfg");
                        break;
                    case "TAZ-TFM":
                        type = pRI("tfm");
                        break;
                    case "TESIS":
                        type = pRI("tesis");
                        break;
                    default:
                        System.err.println("Unknown type property: " + type_string + " for " + path);
                        continue;
                }
                resource.addProperty(RDF.type, type);

                // identifier
                resource.addProperty(pRI("path"), path);

                // date
                addProperty(xmlDoc, resource, FIELD_DATE, "date", XSDDatatype.XSDgYear);

                // description
                addProperty(xmlDoc, resource, FIELD_DESCRIPTION, "data", null);

                // language
                addProperty(xmlDoc, resource, FIELD_LANGUAGE, "language", null);

                // subject
                addProperty(xmlDoc, resource, FIELD_SUBJECT, "data", null);
                // as concepts
//                list = xmlDoc.getElementsByTagName(PREFIX_DC + FIELD_SUBJECT);
//                for (int i = 0; i < list.getLength(); i++) {
//                    String name = list.item(i).getTextContent().strip().replaceAll(" ", "_").toLowerCase();
//                    resource.addProperty(RDF.type, pRIC(name));
//                }

                // title
                addProperty(xmlDoc, resource, FIELD_TITLE, "data", null);

                // relation
//                addProperty(xmlDoc, resource, FIELD_RELATION, "relation", XSDDatatype.XSDanyURI);

                // rights
//                addProperty(xmlDoc, resource, FIELD_RIGHTS, "rights", XSDDatatype.XSDanyURI);


                // publisher
                String publisher = xmlDoc.getElementsByTagName(PREFIX_DC + FIELD_PUBLISHER).item(0).getTextContent();
                String[] uni_dep_ar = publisher.split(";");
                if (uni_dep_ar.length >= 1) {
                    // university
                    resource.addProperty(pRI("publisher"),
                            model.createResource(AnonId.create(uni_dep_ar[0].strip()))
                                    .addProperty(RDF.type, pRI("university"))
                                    .addProperty(pRI("name"), uni_dep_ar[0].strip())
                    );
                }
                if (uni_dep_ar.length >= 2) {
                    // department
                    resource.addProperty(pRI("publisher"),
                            model.createResource(AnonId.create(uni_dep_ar[1].strip()))
                                    .addProperty(RDF.type, pRI("department"))
                                    .addProperty(pRI("name"), uni_dep_ar[1].strip())
                    );
                }
                if (uni_dep_ar.length >= 3) {
                    // area
                    resource.addProperty(pRI("publisher"),
                            model.createResource(AnonId.create(uni_dep_ar[2].strip()))
                                    .addProperty(RDF.type, pRI("area"))
                                    .addProperty(pRI("name"), uni_dep_ar[2].strip())
                    );
                }

                // contributor
                list = xmlDoc.getElementsByTagName(PREFIX_DC + FIELD_CONTRIBUTOR);
                for (int i = 0; i < list.getLength(); i++) {
                    // foreach contributor
                    resource.addProperty(pRI("contributor"),
                            createPerson(model, list.item(i).getTextContent().strip())
                    );
                }

                // creator
                list = xmlDoc.getElementsByTagName(PREFIX_DC + FIELD_CREATOR);
                for (int i = 0; i < list.getLength(); i++) {
                    // foreach creator
                    resource.addProperty(pRI("creator"),
                            createPerson(model, list.item(i).getTextContent().strip())
                    );
                }

            } catch (Exception e) {
                // error reading
                System.err.println("Can't parse file " + file);
                e.printStackTrace();
            }

//            // only 1 file, for testing
//            break;
        }

        // infer new data
        System.out.println("Infer new data");
//        model = ModelFactory.createInfModel(PelletReasonerFactory.theInstance().create(), model);
        model = ModelFactory.createRDFSModel(model);

        // save the model
        System.out.println("Save model");
        model.write(new FileOutputStream(rdfPath), "RDF/XML-ABBREV");

    }

    static private void addProperty(Document document, Resource resource, String tag, String property, RDFDatatype type) {
        NodeList list = document.getElementsByTagName(PREFIX_DC + tag);
        for (int i = 0; i < list.getLength(); i++) {
            if (type == null)
                resource.addProperty(pRI(property), list.item(i).getTextContent().strip());
            else
                resource.addProperty(pRI(property), list.item(i).getTextContent().strip(), type);
        }
    }

    static private Resource createPerson(Model model, String text) {
        Resource person = model.createResource(AnonId.create(text))
                .addProperty(RDF.type, pRI("person"));

        String[] last_first = text.split(",");
        if (last_first.length >= 2) {
            // has first and last name
            person.addProperty(pRI("lastName"), last_first[0].strip());
            person.addProperty(pRI("firstName"), last_first[1].strip());
        } else {
            // only has firstname
            person.addProperty(pRI("firstName"), last_first[0].strip());
        }
        return person;
    }
}
