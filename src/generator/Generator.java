package generator;

import java.io.DataOutputStream;
import java.io.IOException;

public class Generator {
    DataOutputStream output;
    int magic=0x72303b3e;
    int version=1;
    Generator(DataOutputStream output){
        this.output=output;
    }
    private void generateo0() throws IOException {
        output.writeInt(this.magic);
        output.writeInt(this.version);
        generateGlobals();
    }
    private void generateGlobals(){

    }
};