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

## clipPolygon2
### Graph
```
======== clipPolygon2_ ========

start
|
m_extent.getWidth() == 0 || m_extent.getHeight() == 0
|   |
|   exit
|
for int iclip_plane = 0; !b_all_outside && iclip_plane < 4; iclip_plane++
.   |
.   switch (iclip_plane)
.   .   .   .   .   .
.   .   .   .   .   case0
.   .   .   .   .   |   ?
.   .   .   .   .   |   assert
.   .   .   .   .   /
.   .   .   .   .
.   .   .   .   case1
.   .   .   .   |   ?
.   .   .   .   |   assert
.   .   .   .   /
.   .   .   .
.   .   .   case2
.   .   .   |   ?
.   .   .   |   assert
.   .   .   /
.   .   .
.   .   case3
.   .   |   ?
.   .   |   assert
.   .   /
.   |
.   !b_intersects_plane
.   |   |   (for loop again?)   -------/\
.   |   \
.   |
.   for int path = m_shape.getFirstPath(m_geometry); path != -1;
.   .   |
.   .   do
.   .   .   |
.   .   .   segment == null
.   .   .   |   |
.   .   .   |   /
.   .   .   |
.   .   .   seg_plane_intersection_status == -1
.   .   .   |   |
.   .   .   |   count > 0
.   .   .   |   |   |
.   .   .   |   .   /
.   .   .   |   ?
.   .   .   |   assert (count == 0)
.   .   .   |   |   |
.   .   .   |   |   exit
.   .   .   |   /
.   .   .   |
.   .   .   for int i = 0; i < split_count; i++
.   .   .   .   |
.   .   .   .   sub_seq == null
.   .   .   .   |   |
.   .   .   .   |   /
.   .   .   .   |
.   .   .   .   sub_segment_plane_intersection_status == -1
.   .   .   .   |   |
.   .   .   .   |   !b_axis_x
.   .   .   .   |   |   .   ?
.   .   .   .   |   |   .   assert ((pt_1.x < clip_value && pt_2.x > clip_value) || (pt_1.x > clip_value && pt_2.x < clip_value))
.   .   .   .   |   |   |
.   .   .   .   |   |   d_1 < d_2
.   .   .   .   |   |   |   |
.   .   .   .   |   |   .   /
.   .   .   .   |   |   /
.   .   .   .   |   |
.   .   .   .   |   |   ?
.   .   .   .   |   |   assert ((pt_1.x < clip_value && pt_2.x > clip_value) || (pt_1.x > clip_value && pt_2.x < clip_value))
.   .   .   .   |   |
.   .   .   .   |   d_1 < d_2
.   .   .   .   |   |   |
.   .   .   .   |   .   /
.   .   .   .   |   /
.   .   .   .   |
.   .   .   .   sub_seg == null
.   .   .   .   |   |
.   .   .   .   |   /
.   .   .   .   |
.   .   .   .   |   ?
.   .   .   .   |   assert (sub_segment_plane_intersection_status != -1)
.   .   .   .   |
.   .   .   .   firstinside == -1
.   .   .   .   |   |
.   .   .   .   |   /
.   .   .   .   |
.   .   .   .   old_inside == 0 && inside == 1
.   .   .   .   |   |   |
.   .   .   .   .   .   /
.   .   .   .   .   /
.   .   .   .   |
.   .   .   .   inside == 1
.   .   .   .   |   |
.   .   .   .   |   /
.   .   .   .   |
.   .   .   .   /   (for loop again?)   -------/\
.   .   .   |
.   .   .   |
.   .   .   split_count == 0
.   .   .   |   |
.   .   .   |   firstinside == -1
.   .   .   |   |   |
.   .   .   |   |   /
.   .   .   |   |
.   .   .   |   old_inside == 0 && inside == 1
.   .   .   |   |   |   |
.   .   .   |   |   .   /
.   .   .   |   |   /
.   .   .   |   |
.   .   .   |   inside == 1
.   .   .   |   |   |
.   .   .   |   |   /
.   .   .   |   /
.   .   .   |
.   .   .   do the while (vertex != first)
.   .   .   /
.   .   |
.   .   firstinside == 0 && inside == 0
.   .   |   |
.   .   |   /
.   .   |
.   .   int i = 0, n = delete_candidates.size(); i < n; i++
.   .   |
.   .   |   (for loop again?)   -------/\
.   .   |
.   .   m_shape.getPathSize(path) < 3
.   .   |   |
.   .   |   /
.   .   |
.   .   /
.   /
|   (for loop again?)   -------/\
|
b_all_outside
|   |
|   exit
|
densify_dist > 0
|   |
|   /
|
exit
------------
```
### Calculations

======== clipPolygon2_ ========

E: 52 maybe?
n = 29 maybe?
M = 52 - 29 + 2 = 25?

seems to be quite off from the correct answer. for loops should probably be counted more 

