package edu.uic.bitslab.propcov.extensions.util;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;

/**
 * A utility class for loading XML files and unmarshalling them into Java objects of a specified type.
 *
 * @param <T> The type of the object into which the XML will be unmarshalled.
 */
public class LoadXML<T> {
    private static final String XML_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String SAX_VALIDATION = "http://xml.org/sax/features/validation";
    private final Class<T> clazz;

    /**
     * Constructor to initialize the LoadXML utility for a specified class type.
     * This utility facilitates loading XML files and unmarshalling them into objects
     * of the provided type.
     *
     * @param clazz The class type into which the XML content will be unmarshalled.
     *              It must correspond to the structure defined within the XML.
     */
    public LoadXML(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Loads an XML file from the provided file path and unmarshals it into a Java object of the specified type.
     *
     * @param filePath the path to the XML file that needs to be unmarshalled
     * @return an object of type {@code T}, representing the unmarshalled XML content
     * @throws JAXBException if an error occurs during JAXB operations
     * @throws SAXException if an error occurs during the XML parsing process
     * @throws ParserConfigurationException if a configuration error is encountered while creating the XML parser
     * @throws FileNotFoundException if the specified file cannot be located
     */
    public T load(Path filePath) throws JAXBException, SAXException, ParserConfigurationException, FileNotFoundException {
        JAXBContext jc = JAXBContext.newInstance(clazz);

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature(XML_LOAD_EXTERNAL_DTD, false);
        spf.setFeature(SAX_VALIDATION, false);

        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        InputSource inputSource = new InputSource(new FileReader(filePath.toFile()));
        SAXSource source = new SAXSource(xmlReader, inputSource);

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        //noinspection unchecked
        return (T) unmarshaller.unmarshal(source);
    }
}
