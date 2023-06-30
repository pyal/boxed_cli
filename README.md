# boxed_cli
Spark / java utils to simplify writing programs in interacting black box paradigm

# BOX

Google JSon  - allows to read / save objects with fields annotated with
@Expose
to json format.
Still there is a problem - JSon - knows nothing of type of saved object.
To be able to save / read objects in JSon format - added Class Box

It have field "TYPE" - where class name is stored.
This allows to save / read arbitrary class child of BOX, in json format (not knowing its type in advance)

Usage can be found in tests: org.boxed.cli.json.BoxTest

# CLI  

For command line usage - using args4j under the hood. Added sugar to simplify usage for java / scala
Adds -h - auto help parameter generation, 
-d - debug level settings for the program
simplify multi task job jeneration

single task - example in org.boxed.cli.run.Sample  

multi task job - example in org.boxed.cli.run.MultiSample 

( akka git - with different modes: brunch / clone / ... - all of them having help, descriptioni and params)


To run examples use
cd $REPODIR
mvn install
java -cp target/boxed_cli-1.0.1-jar-with-dependencies.jar org/boxed/cli/run/{Sample,MultiSample}

Examples show main object content after parameter parsing  using Box functionality.
