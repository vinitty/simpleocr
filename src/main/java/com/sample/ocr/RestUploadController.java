/**
 * 
 */
package com.sample.ocr;

import static org.bytedeco.javacpp.lept.pixDestroy;
import static org.bytedeco.javacpp.lept.pixRead;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.lept.PIX;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * @author vinit
 *
 */
@RestController
public class RestUploadController {

    private final Logger logger = LoggerFactory.getLogger(RestUploadController.class);

    private static String UPLOADED_FOLDER = System.getProperty("java.io.tmpdir");
    @Autowired
    private TessBaseAPI tessBaseAPI;
    @PostMapping("/api/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile uploadfile) {

        logger.debug("Single file upload!");

        if (uploadfile.isEmpty()) {
            return new ResponseEntity("please select a file!", HttpStatus.OK);
        }

        try {

            saveUploadedFiles(Arrays.asList(uploadfile));

        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity("Successfully uploaded - " +
                uploadfile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);

    }

    @PostMapping("/api/upload/multi")
    public ResponseEntity<?> uploadFileMulti(
            @RequestParam("extraField") String extraField,
            @RequestParam("files") MultipartFile[] uploadfiles) {

        logger.debug("Multiple file upload!");
    	List<Map<String, Map<String, String>>> list = new ArrayList<>();

        // Get file name
        String uploadedFileName = Arrays.stream(uploadfiles).map(x -> x.getOriginalFilename())
                .filter(x -> !StringUtils.isEmpty(x)).collect(Collectors.joining(" , "));

        if (StringUtils.isEmpty(uploadedFileName)) {
            return new ResponseEntity("please select a file!", HttpStatus.OK);
        }

        try {

            //saveUploadedFiles(Arrays.asList(uploadfiles));
            List<String>  paths = saveUploadedFiles(Arrays.asList(uploadfiles));
	    	 for(String str : paths) {
	    		 if(str.endsWith(".jpeg")
	    		   || str.endsWith(".png")
	    		   || str.endsWith(".bmp")
	    		   || str.endsWith(".jpeg")
	    		   || str.endsWith(".gif")
	    		   || str.endsWith(".img")
	    		   || str.endsWith(".psd"))
	    		 list.add(getDataFromImages(str));
	    		 else
	    		 list.add(getDataFromFiles(str));
	    	 }

        } catch (IOException e) {
			e.printStackTrace();

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		}

        return new ResponseEntity(list
                + uploadedFileName, HttpStatus.OK);

    }

    // 3.1.3 maps html form to a Model
    @PostMapping("/api/upload/multi/model")
    public ResponseEntity<?> multiUploadFileModel(@ModelAttribute UploadModel model) {

        logger.debug("Multiple file upload! With UploadModel");
    	List<Map<String, Map<String, String>>> list = new ArrayList<>();

        try {

        	List<String>  paths = saveUploadedFiles(Arrays.asList(model.getFiles()));
	    	 for(String str : paths) {
	    		 if(str.endsWith(".jpeg")
	    		   || str.endsWith(".png")
	    		   || str.endsWith(".bmp")
	    		   || str.endsWith(".jpeg")
	    		   || str.endsWith(".gif")
	    		   || str.endsWith(".img")
	    		   || str.endsWith(".psd"))
	    		 list.add(getDataFromImages(str));
	    		 else
	    		 list.add(getDataFromFiles(str));

	         }
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		}
        return new ResponseEntity(list, HttpStatus.OK);

    }

    //save file
    private List<String> saveUploadedFiles(List<MultipartFile> files) throws IOException {
    	List<String> paths = new ArrayList<>();
        for (MultipartFile file : files) {

            if (file.isEmpty()) {
                continue; //next pls
            }

            byte[] bytes = file.getBytes();
            Path path = Paths.get(UPLOADED_FOLDER + file.getOriginalFilename());
            Files.write(path, bytes);
            System.out.println(path.toString());
            paths.add(path.toString());
            

        }
        return paths;
    }
    
    
    public Map<String, Map<String, String>> getDataFromImages(String path) throws Exception {
    	Map<String, Map<String, String>> map  = new HashMap<>();
        BytePointer outText;
        // Open input image with leptonica library
        PIX image = pixRead(path);
        tessBaseAPI.SetImage(image);
        // Get OCR result
        outText = tessBaseAPI.GetUTF8Text();
        String string = outText.getString();
        System.out.println("OCR output:\n" + string);
    	Map<String, String> tempmap  = new HashMap<>();
    	tempmap.put("content", string);
        map.put("data", tempmap);
        // Destroy used object and release memory
        tessBaseAPI.End();
        outText.deallocate();
        pixDestroy(image);
        return map;
    }
    
    public Map<String, Map<String, String>> getDataFromFiles(String path) throws Exception {
    	Map<String, Map<String, String>> map  = new HashMap<>();
    	Map<String, String> tempmap  = new HashMap<>();
        Parser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new  BodyContentHandler();
        InputStream stream = new FileInputStream(path); 
        parser.parse(stream, handler, metadata, new ParseContext());
        for(String name : Arrays.asList(metadata.names())) {
            System.out.println(name +": " + metadata.get(name));
        	tempmap.put(name, metadata.get(name));
        }
    	tempmap.put("content", handler.toString());
        map.put("data", tempmap);
        System.out.println("content: " + handler.toString());
        return map;
    }
}

