package sqlmapper;

public class IncompleteFormError extends Exception{
    public IncompleteFormError(String failure){
        super(failure);
    }
    
}
