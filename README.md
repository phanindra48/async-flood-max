# Asynchronous FloodMax algorithm for leader election

### Input file format:
1.	No of processes in the graph
2.	IDs of the processes
3.	Connectivity matrix of the graph

Three such input files (for three graphs) are created:
* connectivity.txt
* connectivity2.txt
* connectivity3.txt

Note: To generate more input files use the generator code

### Steps to run
These files can be read one after the other to get the leaders of those graphs.

After giving the file to be taken as input in the code, we can compile and run the code by executing the following commands in the command prompt.

```bash
Compile: javac -d out -sourcepath src src/pxp180031/Main.java 
Run: java -cp out pxp180031.Main
```
