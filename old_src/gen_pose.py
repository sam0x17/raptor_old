# -*- coding: utf-8 -*-
import bpy
import os
import random
import math
import sys
import ntpath
import subprocess
import mathutils

print()
print("RAPTOR Blender hook loaded")

pi = 3.14159265
deg_to_rad = 0.0174532925
rad_to_deg = 57.2957795

# utility functions:
# compute 3D distance between two points
def dist3d(v1, v2):
	xd = v2[0] - v1[0]
	yd = v2[1] - v1[1]
	zd = v2[2] - v1[2]
	return math.sqrt(xd * xd + yd * yd + zd * zd)

# divides a 3D vector by a scalar
def v3d_div_scal(v1, s1):
	return ((v1[0] / s1, v1[1] / s1, v1[2] / s1))

# divides a scalar by a 3D vector
def scal_div_v3d(s1, v1):
	return ((s1 / v1[0], s1 / v1[1], s1 / v1[2]))

# adds a scalar and a 3D vector
def v3d_add_scal(v1, s1):
	return ((v1[0] + s1, v1[1] + s1, v1[2] + s1))

# adds a scalar and a 3D vector
def scal_add_v3d(s1, v1):
	return v3d_add_scal(v1, s1)

# adds a 3D vector to a 3D vector
def v3d_add_v3d(v1, v2):
	return ((v1[0] + v2[0], v1[1] + v2[1], v1[2] + v2[2]))

# subtracts a 3D vector from a 3D vector
def v3d_sub_v3d(v1, v2):
	return ((v1[0] - v2[0], v1[1] - v2[1], v1[2] - v2[2]))

# multiplies a 3D vector by a 3D vector (not cross product)
def v3d_mul_v3d(v1, v2):
	return ((v1[0] * v2[0], v1[1] * v2[1], v1[2] * v2[2]))

# divides a 3D vector by a 3D vector
def v3d_div_v3d(v1, v2):
	return ((v1[0] / v2[0], v1[1] / v2[1], v1[2] / v2[2]))

# subtracts a scalar from a 3D vector
def v3d_sub_scal(v1, s1):
	return ((v1[0] - s1, v1[1] - s1, v1[2] - s1))

# subtracts a 3D vector from a scalar
def scal_sub_v3d(s1, v1):
	return ((s1 - v1[0], s1 - v1[1], s1 - v1[2]))

# multiplies a 3D vector by a scalar
def v3d_mul_scal(v1, s1):
	return ((v1[0] * s1, v1[1] * s1, v1[2] * s1))

# multiplies a 3D vector by a scalar
def scal_mul_v3d(s1, v1):
	return v3d_mul_scal(v1, s1)

# compute 3D magnitude of v1
def mag3d(v1):
	return math.sqrt(v1[0] * v1[0] + v1[1] * v1[1] + v1[2] * v1[2])

def unit3d(v1):
	return v3d_div_scal(v1, mag3d(v1))

def v3d_floor(v1):
	return ((math.floor(v1[0]), math.floor(v1[1]), math.floor(v1[2])))

def normrot(v1):
	v2 = v3d_mul_scal(v3d_floor(v3d_div_scal(v1, deg_to_rad * 360.0)), deg_to_rad * 360.0)
	return v3d_sub_v3d(v1, v2)

# end utility functions

# delete default cube
bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.object.select_by_type(type='MESH')
bpy.ops.object.delete(use_global=False)
for item in bpy.data.meshes:
	bpy.data.meshes.remove(item)

# load 3ds file
raptor_3dsfile = os.getenv('raptor_3dsfile')
print("Source 3DS file: " + raptor_3dsfile)
bpy.ops.import_scene.autodesk_3ds(filepath=raptor_3dsfile, filter_glob="*.3ds", constrain_size=50.0, use_image_search=True, use_apply_transform=True, axis_forward='Y', axis_up='Z')

# get free rotation variables
free_rot_vars = os.getenv('free_rot_vars')
x_free = 'x' in free_rot_vars
y_free = 'y' in free_rot_vars
z_free = 'z' in free_rot_vars
print("Free Rotation Variables: " + free_rot_vars)

# set up scene and render variables
scene = bpy.context.scene
render = scene.render

# select all meshes
bpy.ops.object.select_by_type(type='MESH')


# average world location of component objects
avg_loc = ((0.0, 0.0, 0.0)) 

# average of component bounding boxes
avg_bb = ((0.0, 0.0, 0.0))

# true center of overall object in object coordinates
center_obj = ((0.0, 0.0, 0.0))

# true center of overall object in world coordinates
center_world = ((0.0, 0.0, 0.0))

# true bounding box
true_bb = ((0.0, 0.0, 0.0))

# value of maximum dimension
max_dim = 0.0

# computes above statistics on selected object(s)
def compute_bb():
	global avg_loc, avg_bb, center_obj, center_world, true_bb, max_dim
	bb_x = 0.0
	bb_y = 0.0
	bb_z = 0.0
	loc_x = 0.0 
	loc_y = 0.0
	loc_z = 0.0
	mb_x = 0.0
	mb_y = 0.0
	mb_z = 0.0
	num_objects = 0
	for obj in bpy.context.selected_objects:
		obj_dim = obj.dimensions
		bb_x += obj_dim[0]
		bb_y += obj_dim[1]
		bb_z += obj_dim[2]
		mb_x = max(obj_dim[0], mb_x)
		mb_y = max(obj_dim[1], mb_y)
		mb_z = max(obj_dim[2], mb_z)
		obj_loc = obj.location.xyz
		loc_x = obj_loc[0]
		loc_y = obj_loc[1]
		loc_z = obj_loc[2]
		num_objects += 1
	bb_x = bb_x / num_objects
	bb_y = bb_y / num_objects
	bb_z = bb_z / num_objects
	loc_x = loc_x / num_objects
	loc_y = loc_y / num_objects
	loc_z = loc_z / num_objects
	avg_loc = ((loc_x, loc_y, loc_z))
	avg_bb = ((bb_x, bb_y, bb_z))
	center_obj = ((bb_x / 2.0, bb_y / 2.0, bb_z / 2.0))
	center_world = ((center_obj[0] + loc_x, center_obj[1] + loc_y, center_obj[2] + loc_z))
	true_bb = ((mb_x, mb_y, mb_z))
	max_dim = max(mb_x, mb_y, mb_z)

# get v3d
def areas_tuple():
    res = {}                                                               
    count = 0
    for area in bpy.context.screen.areas:                                  
        res[area.type] = count                                             
        count += 1
    return res
areas = areas_tuple()
view3d = bpy.context.screen.areas[areas['VIEW_3D']].spaces[0]


# save original locations
orig_camera_loc = ((scene.camera.location[0], scene.camera.location[1], scene.camera.location[2]))
orig_obj_rots = {}
for obj in bpy.context.selected_objects:
	orig_obj_rots[obj.name] = obj.rotation_euler


# fit model to viewport
compute_bb()
scale_factor = 8.0 / max_dim
view3d.pivot_point = "CURSOR"
view3d.cursor_location = center_world
for obj in bpy.context.selected_objects:
	obj.scale = ((scale_factor * obj.scale[0], scale_factor * obj.scale[1], scale_factor * obj.scale[2]))
bpy.ops.object.transform_apply(scale=True)
compute_bb()

first_run = True


def render_pose(rot, dist_factor, light_energy, width, height, filename):
	global avg_loc, avg_bb, center_obj, center_world, true_bb, max_dim, view3d, pi, rad_to_deg, deg_to_rad, scene, render, orig_camera_loc, orig_obj_rots, first_run

	scene.camera.location = orig_camera_loc

	for obj in bpy.context.selected_objects:
		if first_run == False:
			obj.rotation_euler = v3d_sub_v3d(obj.rotation_euler, orig_obj_rots[obj.name])
		obj.rotation_euler = normrot(v3d_add_v3d(rot, obj.rotation_euler))
		orig_obj_rots[obj.name] = rot
	bpy.ops.object.transform_apply(rotation=True)
	first_run = False

	compute_bb()

	initial_dist = dist3d(scene.camera.location, center_world)
	extra_dist = initial_dist * dist_factor
	scene.camera.location = v3d_add_v3d(v3d_mul_scal(unit3d(orig_camera_loc), extra_dist), orig_camera_loc)
	final_dist_proper = initial_dist + extra_dist
	final_dist_actual = dist3d(center_world, scene.camera.location)
	scene.camera.data.clip_end = final_dist_actual + max_dim * 2.0

	bpy.data.worlds['World'].light_settings.use_environment_light = True
	bpy.data.worlds['World'].light_settings.environment_energy = light_energy
	render.resolution_x = width
	render.resolution_y = height
	render.resolution_percentage = 100
	render.alpha_mode = 'PREMUL' #'TRANSPARENT'
	render.bake_type = 'ALPHA'
	render.image_settings.color_mode = 'RGBA'
	render.use_antialiasing = True
	render.image_settings.file_format='PNG'
	render.filepath = filename
	true_dist = initial_dist + extra_dist
	print("TRUE_DIST: " + str(true_dist))
	bpy.ops.render.render(write_still = True)
	return true_dist

def gen_random_pose(min_dist_factor, max_dist_factor, min_lighting, max_lighting):
	global x_free, y_free, z_free
	rot = ((0.0, 0.0, 0.0))
	if x_free:
		rot = ((random.uniform(0.0, 360.0) * deg_to_rad, rot[1], rot[2]))
	if y_free:
		rot = ((rot[0], random.uniform(0.0, 360.0) * deg_to_rad, rot[2]))
	if z_free:
		rot = ((rot[0], rot[1], random.uniform(0.0, 360.0) * deg_to_rad))

	dist_factor = random.uniform(min_dist_factor, max_dist_factor)
	light_energy = random.uniform(min_lighting, max_lighting)
	return {'rot': rot, 'dist_factor': dist_factor, 'light_energy': light_energy}


for i in range(90001, 100000):
	pose = gen_random_pose(0.0, 5.0, 0.0, 0.65)
	model_name = str.replace(ntpath.basename(raptor_3dsfile.lower()), '.3ds', '')
	img_filename = "DATA/" + model_name + "_%06d.png" % i
	annotation_filename = "DATA/" + model_name + "_%06d.txt" % i
	width = 1800
	height = 1800
	dest_width = 128
	dest_height = 128
	true_dist = render_pose(pose['rot'], pose['dist_factor'], pose['light_energy'], width, height, img_filename)
	rx = pose['rot'][0]
	ry = pose['rot'][1]
	rz = pose['rot'][2]
	ang = mathutils.Euler((rx, ry, rz))
	v = mathutils.Vector((1.0, 1.0, 1.0))
	v.rotate(ang)
	v = unit3d(v)
	rx = v[0]
	ry = v[1]
	rz = v[2]
	print("processing image and generating annotations...")
	subprocess.call(['java', '-cp', '.:imgscalr-lib-4.2.jar', '-Xmx4024M', '-Xms4024M', 'prepare_image', img_filename, annotation_filename, str(true_dist), str(rx), str(ry), str(rz), str(dest_width), str(dest_height)])

