# Report for assignment 3

## Project

Name: Esri Geometry API Java

URL: https://github.com/Esri/geometry-api-java

The Esri Geometry API for Java enables developers to write custom applications for analysis of spatial data. This API is used in the Esri GIS Tools for Hadoop and other 3rd-party data processing solutions.

## Onboarding experience

The onboarding experience was overall pleasant. The documentation explained the three simple tests to build and run the API: clone the repo, `mvn compile` and `mvn test`. The only requirements for this is a Java JDK 1.6 or greater and Maven, which all team members already had installed on their systems. All dependencies were handled by Maven, which automatically installed missing components for us. The only problem we had with the onboarding was that one of the tests did not work on two of the group members’ systems. We decided to continue with this project since everything worked as expected when this test was commented out.

## Complexity

Choice of functions (and counts by Lizard):

1. \_Cut (Cutter.java) (nloc: 509 , cc: 106)
2. geodesic_distance_ngs (GeoDist.java) (nloc: 270, cc: 53)
3. exportMultiPathToESRIShape (OperatorExportToESRIShapeCursor.java) (nloc: 247, cc: 85)
4. importFromWkbPolygon (OperatorImportFromWkbLocal.java) (nloc: 271, cc: 81)
5. tryFastIntersectPolylinePolygon\_ (OperatorIntersectionCursor.java) (nloc: 278, cc: 48)

### Results (Manually counted CC)

1. 107
2. 49
3. 82
4. 81
5. 44

### Questions

1. What are your results for five complex functions?

   The results that we got is that the manual count of CC is very close to the expected CC given by the tool. We believe that the reason why it differs slightly is that the tool does not take exit points into account. The tool seems to always count on one exit point for each function, which makes our manual count on functions with many return statements lower than the tool. We used the formula: $ DecisionPoints - ExitPoints + 2 $ to count CC.

2. Are the functions just complex, or also long?

   The functions are both long and complex, as they all have more than 230 non-comment lines of code each.

3. What is the purpose of the functions?

- Function 1 - \_Cut

  Used to split a polyline or polygon geometry into two or more parts along a cutting line. This cutting line is typically another polyline or polygon geometry that intersects the target geometry.

- Function 2 - geodesic_distance_ngs

  The purpose of this function is to calculate the distance between two points on the surface of an object (like distance between two coordinates on earth)

- Function 3 - exportMultiPathToESRIShape

  Used to convert Esri multi-path geometries into Esri shape format.

- Function 4 - importFromWkbPolygon
  Used to import polygons from the Well-Known Binary (WKB) format into Esri geometry objects. WKB is a binary representation of geometries that is often used for efficient storage and transmission of spatial data.

- Function 5 - tryFastIntersectPolylinePolygon\_

  The purpose of the function is to determine the intersection between a polyline and a polygon in a fast way (parameters therefore consist of a polygon and polyline). A check is performed to see if it makes sense to do a fast check or not.

  It is to no surprise that all of these functions have very high complexity as they are all developed to function on a broad range of different geometric bodies. These bodies can differ a lot from each other and therefore introduce a lot of different edge cases. The functions need to handle these edge cases, leading to a lot of conditional statements to catch them, which in turn leads to a high complexity.

4. Are exceptions taken into account in the given measurements?

   No neither in our calculation nor in lizard’s.

5. Is the documentation clear w.r.t. all the possible outcomes?

   The documentation of the functions are close to non-existent. So it isn’t clear.

## Refactoring

Function 1 - \_Cut

Refactoring of this function could involve breaking down the function into smaller methods, each responsible for a specific aspect, such as path processing, vertex processing, and initialization. The logic for creating a new multipath and adding it to ‘cutPairs’ or incrementing ‘segmentCount’ is repeated in multiple places within the function. A new method could be called that handles multipaths, replacing long series of if-statements and making the function more concise.

Function 2 - geodesic_distance_ngs

There are alot of big if statements as well as one big while-loop. These could/should be broken into separate functions. There are if-statements for if the points are perfectly antipodal, inside of this there are in turn two big if-statements (if they are on the poles or not.) containing multiple small if-statements. I would make these their own functions, just one if-statement if they are antipodal and then a separate function where the calculations are made.

There is also a big if-statement (containing multiple small if-statements) that performs calculations if the shape is a sphere. I would also break this into its own function. There is a big while-loop performing calculations for “top of the long-line loop (kind = 1) “. I would also break this into its own function. Lastly there are if-statements for the convergence. I would break this whole part into its own function.

Function 3 - exportMultiPathToESRIShape

A part of the function begins by checking if bPolygon is false, and then updates a variable accordingly. This part could be made into its own function with bPolygon and the variable to be updated as parameters and then return the for the updated value. This reduces the cyclomatic complexity by 5. Another part of the function checks different conditions and based on that assigns the variable type a value. By making this part into a separate function and return the variable type it would reduce the cyclomatic complexity by 20.

Function 4 - importFromWkbLocal

The function begins by a nested for-loop that finds the total point count and part count. It continues with another nested for-loop that reads Coordinates. These two loops are the biggest reason behind function 4s high cyclomatic complexity, as they have 24 and 34 decisions being made in them respectively. As these loops do not contain any exit points, they could be made into their own functions to greatly reduce the complexity of the function. These two helper functions would need three of function 4’s parameters (wkbHelper, bZs and bMs) as well as the polygon being created, and return the polygon with updated values. This would result in a CC of 23, which is still slightly high but an improvement of 72% from the previous CC of 81.

Function 5 - tryFastIntersectPolylinePolygon

The function can be refactored by removing unnecessary branches such as if(true) at the beginning of the function. Accelerator logic could also be extracted to another function, as it has a high cyclomatic complexity. There is also a case when there is no intersection, since both points are inside. The logic for when an intersection does not happen could be moved out of the main function.

## Coverage

### Tools

We used JaCoCo. It was very easy to implement through Maven. All you had to do was add the JaCoCo plugin in the pom.xml file and everything else is done automatically when running `mvn test`.

### Your own coverage tool

Our own coverage tool is found in CoverageSummary.java. It is a rudimentary tool that works by manually adding helper-function calls to each branch in the functions you want to measure. These helper functions write the unique ID for the branch to a txt-file unique for the function, which is cleared every time the project is tested. After testing is finished, the checkCoverageFromFile function in CoverageSummary reads these files and identifies which branches that have or haven’t been written to the file. These results are summarized and printed to the terminal.

### Evaluation

1. How detailed is your coverage measurement?

   Our tool is quite detailed. It covers all common branches (if-else, loops, switch-cases…) as well as ternary operators.

2. What are the limitations of your own tool?

   A major limitation is the need to manually add a line of code after every branching point. This makes it possible for mistakes to happen as well as making it less dynamic. The tool also does not take exceptions into account.

3. Are the results of your tool consistent with existing coverage tools?

   They are quite similar but JaCoCo covers a bit more:

   | **Function** | **Ours** | **JaCoCo** |
   | ------------ | -------- | ---------- |
   | 1            | 60%      | 53%        |
   | 2            | 34%      | 35%        |
   | 3            | 60%      | 54%        |
   | 4            | 91%      | 71%        |
   | 5            | 57%      | 45%        |

## Coverage improvement

Report of old coverage: (on master branch)

`absolute-path-to-project/target/site/jacoco/index.html`

Report of new coverage: (on feat/improve-test-coverage branch)

`absolute-path-to-project/target/site/jacoco/index.html`

| **Function**                  | **Before** | **After** | **Num. tests added** |
| ----------------------------- | ---------- | --------- | -------------------- |
| 1                             | 60%        |           |                      |
| 2                             | 34%        | 74%       | 14                   |
| 3                             | 60%        |           |                      |
| 4                             | 91%        | -         | -                    |
| 5                             | 57%        | -         | -                    |
| intersectionWithAxis2D (Line) | 50%        | 79%       | 4                    |

## Overall experience

We learnt how coverage tools are used and the importance of good unit testing. We also gained a deeper understanding of metrics used in software development, such as cyclomatic complexity as well as tools such as Lizard used for measuring this.

## Self-assessment: Way of working

After careful assessment we agree that our way of working is in the “Foundation Established” state. We feel we have passed the “Principles Established” state since we set up a clear working space where we collectively use Github and JUnit. We split up the workload and assigned each part of the work to a member of the group. We all understood our assignments clearly and we knew the constraints, e.g. working on different branches, had to resolve merge conflicts etc.

We feel that we are in the “Foundation Established” state since the key practices and tools for our way of working have been selected, i.e. how work is divided and how commit messages should look. These tools and practices have been integrated to form a usable way of working. Work has started and all non-negotiable practices have been agreed upon. We also feel that after this assignment we have a good understanding of what is needed to execute the desired way of working, and where the capability of our team lies in comparison to that.

Our assessment of our working state is that we have not yet reached the “In Use” state. Mainly because we don’t “regularly inspect the practices and tools selected” and we don’t have any procedures to handle feedback on our way of working.

Since the last assessment however we feel that we have improved our way of communication and that we have “regular communication between the group members during our work period”.
