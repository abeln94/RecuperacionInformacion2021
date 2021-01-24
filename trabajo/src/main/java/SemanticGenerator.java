import indexfiles.extractor.Extractor;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import tools.ArgsParser;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static indexfiles.parser.RecordsDcParser.*;

public class SemanticGenerator {

    // uris
    private static final Model m = ModelFactory.createDefaultModel();
    public static final String riUri = "http://rdf.unizar.es/recuperacion_informacion/grupo_110/modelo#";
    public static final String ricUri = "http://rdf.unizar.es/recuperacion_informacion/grupo_110/conceptos#";

    public static Property pRI(String name) {
        return m.createProperty(riUri, name);
    }

    public static Property pRIC(String name) {
        return m.createProperty(ricUri, name);
    }

    // arguments
    private static String rdfPath;
    private static String skosPath;
    private static String owlPath;
    private static String docsPath;
    private static Boolean test = false;

    public static void main(String[] args) throws FileNotFoundException {

        // parse arguments
        new ArgsParser("Converts a recordsdc collection into a turtle file")
                .addRequired("-rdf", "the rdf file where to generate the final graph", 1, v -> rdfPath = v.get(0))
                .addRequired("-skos", "the rdf file of the skos model", 1, v -> skosPath = v.get(0))
                .addRequired("-owl", "the rdf file of the owl model of the collection", 1, v -> owlPath = v.get(0))
                .addRequired("-docs", "the folder with the files to parse", 1, v -> docsPath = v.get(0))
                .addOptional("-test", "flag to try things", 0, v -> test = true)
                .parse(args);

        if (test) {
            try {
                // convert SKOS
                String content = Files.readString(Path.of(owlPath));

                Matcher m = Pattern.compile("ric:([^ .;]*)").matcher(content);
                while (m.find()) {
                    String original = m.group(1);
                    if (original == null || original.isEmpty()) continue;
                    String replacement = tokenizeString(new SimpleAnalyzer(), original).get(0);
                    content = content.replaceAll(original, replacement);
                }
                Files.writeString(Path.of(owlPath), content);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // load the models
        Model model = FileManager.get().loadModel(skosPath, "TURTLE");
        model = model.add(FileManager.get().loadModel(owlPath, "TURTLE"));

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
                addTheme(xmlDoc, resource, FIELD_DESCRIPTION, model);

                // language
                addProperty(xmlDoc, resource, FIELD_LANGUAGE, "language", null);

                // subject
                addProperty(xmlDoc, resource, FIELD_SUBJECT, "data", null);
                addTheme(xmlDoc, resource, FIELD_SUBJECT, model);

                // title
                addProperty(xmlDoc, resource, FIELD_TITLE, "data", null);
                addTheme(xmlDoc, resource, FIELD_TITLE, model);

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

            // cap elements, for testing
            if (test && model.size() > 10000) break;
        }

        // infer new data
        System.out.println("Infer new data");
//        Reasoner reasoner = PelletReasonerFactory.theInstance().create();
//        Reasoner reasoner = ReasonerRegistry.getTransitiveReasoner();
        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
//        reasoner = reasoner.bindSchema(modelOWL/*.union(FileManager.get().loadModel("http://www.w3.org/2009/08/skos-reference/skos-owl1-dl.rdf"))*/);
        model = ModelFactory.createInfModel(reasoner, model);
//        model = ModelFactory.createRDFSModel(model);

        // clean
        System.out.println("Clean model");
        model = borrarRecursosInferencia(model);

        // save the model
        System.out.println("Save model");
        model.write(new FileOutputStream(rdfPath), "RDF/XML-ABBREV");

    }

    // ------------------------- utils -------------------------

    static private void addProperty(Document document, Resource resource, String tag, String property, RDFDatatype type) {
        NodeList list = document.getElementsByTagName(PREFIX_DC + tag);
        for (int i = 0; i < list.getLength(); i++) {
            String text = list.item(i).getTextContent().strip();
            if (type == null)
                resource.addProperty(pRI(property), text);
            else
                resource.addProperty(pRI(property), text, type);
        }
    }

    static private void addTheme(Document document, Resource resource, String tag, Model model) {
        NodeList list = document.getElementsByTagName(PREFIX_DC + tag);
        for (int i = 0; i < list.getLength(); i++) {
            String text = list.item(i).getTextContent().strip();
            for (String term : tokenizeString(new SimpleAnalyzer(), text)) {
                if (model.containsResource(pRIC(term))) {
                    resource.addProperty(RDF.type, pRIC(term));
                }
//                resource.addProperty(pRI("data"), term);
            }
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

    /**
     * borramos las clases del modelo owl que se añaden automáticamente al hacer la inferencia
     * borramos las clases del modelo rdfs que se añaden automáticamente al hacer la inferencia
     * simplemente para facilitar la visualización de la parte que nos interesa
     * si quieres ver todo lo que genera el motor de inferencia comenta estas lineas
     */
    private static Model borrarRecursosInferencia(Model inf) {
        //hacemos una copia del modelo ya que el modelo inferido es inmutable
        Model model2 = ModelFactory.createDefaultModel();
        model2.add(inf);
        for (String uri : new String[]{
                "http://www.w3.org/2002/07/owl#topDataProperty",
                "http://www.w3.org/2002/07/owl#topObjectProperty",
                "http://www.w3.org/2002/07/owl#Thing",
                "http://www.w3.org/2002/07/owl#bottomObjectProperty",
                "http://www.w3.org/2002/07/owl#Nothing",
                "http://www.w3.org/2002/07/owl#bottomDataProperty",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#rest",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#List",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#subject",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#Bag",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#first",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#object",
                "http://www.w3.org/2000/01/rdf-schema#Class",
                "http://www.w3.org/2000/01/rdf-schema#label",
                "http://www.w3.org/2000/01/rdf-schema#Resource",
                "http://www.w3.org/2000/01/rdf-schema#ContainerMembershipProperty",
                "http://www.w3.org/2000/01/rdf-schema#isDefinedBy",
                "http://www.w3.org/2000/01/rdf-schema#seeAlso",
                "http://www.w3.org/2000/01/rdf-schema#Container",
                "http://www.w3.org/2000/01/rdf-schema#Datatype",
                "http://www.w3.org/2000/01/rdf-schema#comment",
                "http://www.w3.org/2000/01/rdf-schema#range",
                "http://www.w3.org/2000/01/rdf-schema#subPropertyOf",
                "http://www.w3.org/2000/01/rdf-schema#subClassOf",
                "http://www.w3.org/2000/01/rdf-schema#Literal",
                "http://www.w3.org/2000/01/rdf-schema#domain",
                "http://www.w3.org/2000/01/rdf-schema#nil",
        })
            model2.removeAll(inf.createResource(uri), null, null);


        return model2;
    }

    public static List<String> tokenizeString(Analyzer analyzer, String string) {
        List<String> result = new ArrayList<>();
        try (
                TokenStream stream = analyzer.tokenStream(null, string)) {
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
            stream.end();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
