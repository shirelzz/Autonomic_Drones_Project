package com.dji.sdk.sample.demo.kcgremotecontroller;
//import org.python.util.PythonInterpreter;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.dji.sdk.sample.R;

import jep.Interpreter;
import jep.Jep;
import jep.JepException;
import jep.SubInterpreter;


//public class JythonTest {
//    public static void main(String[] args) {
//        try(PythonInterpreter pyInterp = new PythonInterpreter()) {
//            pyInterp.exec("print('Hello Python World!')");
//        }
//    }
//}
public class JythonTest extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // Initialize Jep and execute Python code
            try (Interpreter jep = new SubInterpreter()) {
                jep.exec("print('Hello from Python!')");
                jep.exec("x = 10 + 5");
                int result = (int) jep.getValue("x");
                Log.d("Jep", "Result from Python: " + result);
            } catch (JepException e) {
                e.printStackTrace();
            }
        }
    }