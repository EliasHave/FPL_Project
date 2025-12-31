package FPL_Code;

public interface AIClient {

    /**
     * Lähettää promptin tekoälylle ja palauttaa vastauksen sellaisenaan
     * @param prompt prompti joka lähetetään tekoälylle
     * @return palauttaa vastauksen sellaisenaan
     * @throws AIClientException
     */
    String sendPrompt(String prompt) throws AIClientException;


//===================================================================================================================================================

    public class AIClientException extends RuntimeException {

        public AIClientException(String message) {
            super(message);
        }

        public AIClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
