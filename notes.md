## Clip
### Graph
```
======== CLIP ========

start
|
geometry.isEmpty()
|   |
|   exit
|
extent.isEmpty()
|   |
|   exit
|
geomtype == Geometry.Type.Point.value()
|   .   |                   
|   .   extent.contains(pt)
|   .   |   |
|   .   |   exit
|   .   exit
|   .
|   geomtype == Geometry.Type.Envelope.value()
|   .   |
|   .   env.intersect(extent)
|   .   |   |
|   .   |   exit
|   .   exit
|   .
|
extent.contains(env_2D)
|   |
|   exit
|
!extent.isIntersecting(env_2D)
|   |
|   exit
|
accel != null
|   |
|   rgeom != null
|   |   |
|   |   hit == RasterizedGeometry2D.HitType.Inside
|   |   |   |
|   |   |   geomtype != Geometry.Type.Polygon.value()
|   |   |   |   |
|   |   |   |   exit
|   |   |   exit
|   |   exit
|   /
|
switch (geomtype)
.   .   .   .
.   .   .   case Geometry.GeometryType.MultiPoint
.   .   .   |   
.   .   .   for int ipoints = 0; ipoints < npoints; ipoints++
.   .   .   .   |
.   .   .   .   !extent.contains(pt)
.   .   .   .   |   |
.   .   .   .   |   ipoints0 == 0
.   .   .   .   |   |   |
.   .   .   .   |   |   /
.   .   .   .   |   |
.   .   .   .   |   ipoints0 < ipoints
.   .   .   .   |   |   |
.   .   .   .   |   |   /
.   .   .   .   |   /
.   .   .   .   /
.   .   .   |
.   .   .   |    (for loop again?)   -------/\
.   .   .   |
.   .   .   ipoints0 > 0
.   .   .   |   |
.   .   .   |   /
.   .   .   |
.   .   .   ipoints0 == 0
.   .   .   |   |
.   .   .   |   exit
.   .   .   exit
.   Geometry.GeometryType.Polygon
.   Geometry.GeometryType.Polyline
.   |
.   exit
.
default
|
exit

------------
```
### Calculations

E: 22
switch
E: 15
E tot: 37

N: 12
switch
N: 8

N tot: 20

M = 37 - 20 + 2 = **19**
should maybe be 23?
some paths were combined if they accessed the same code, like in the switch case
