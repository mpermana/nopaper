package nopaper;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

public class ReadWritePDF {

	public static void createFilledPDF(String filename, String outputFilename,
			Map<String, String> fieldValues) {
		PDDocument pdf = null;
		try {
			pdf = PDDocument.load(filename);
			if (pdf.isEncrypted()) {
				try {
					pdf.decrypt("");
				} catch (InvalidPasswordException e) {
					System.err.println("Error: The document is encrypted.");
				}
			}

			// printFields(pdf);
			PDAcroForm acroForm = pdf.getDocumentCatalog().getAcroForm();
			Iterator<Entry<String, String>> it = fieldValues.entrySet()
					.iterator();
			while (it.hasNext()) {
				Entry<String, String> pairs = it.next();
				PDField setField = acroForm.getField(pairs.getKey());
				setField.setValue(pairs.getValue());
			}
			pdf.save(outputFilename);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				pdf.close();
			} catch (IOException e) {
			}
		}
	}

}
