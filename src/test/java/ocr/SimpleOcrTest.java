package ocr;

import static org.bytedeco.javacpp.lept.pixDestroy;
import static org.bytedeco.javacpp.lept.pixRead;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.lept.PIX;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;
import org.junit.Test;

public class SimpleOcrTest {
    
    @Test
    public void givenTessBaseApi_whenImageOcrd_thenTextDisplayed() throws Exception {
        BytePointer outText;

        TessBaseAPI api = new TessBaseAPI();
        if (api.Init(getClass().getClassLoader().getResource("tessdata").getPath(), "ENG") != 0) {
            System.err.println("Could not initialize tesseract.");
            System.exit(1);
        }

        PIX image = pixRead("test.png");
        api.SetImage(image);
        outText = api.GetUTF8Text();
        String string = outText.getString();
        assertTrue(!string.isEmpty());
        System.out.println("OCR output:\n" + string);

        api.End();
        outText.deallocate();
        pixDestroy(image);
    }
    
    @Test
    public void testApiForPDF() throws Exception {
        Parser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new  BodyContentHandler();
        InputStream stream = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("test.pdf")); 
        parser.parse(stream, handler, metadata, new ParseContext());
        stream.close();
        for(String name : Arrays.asList(metadata.names())) {
            System.out.println(name +": " + metadata.get(name));

        }
        System.out.println("content: " + handler.toString());
        
       

        
    }
}