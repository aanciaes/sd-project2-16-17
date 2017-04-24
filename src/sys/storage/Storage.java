package sys.storage;

import java.util.List;

import api.Document;

public interface Storage {

	// Retrieves the list of urls of the documents that have all the keywords being searched
	List<Document> search( List<String> keywords );
	
	//Adds the given document to the stored index
	boolean store( String id, Document doc );
	
	//Removes the given document from the index.
	boolean remove( String id );
	
}
