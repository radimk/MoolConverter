CREATE OR REPLACE VIEW gradle.project_mapping AS (
	SELECT bld_id, array_to_string(bld_path, '-') AS bld_path, string_agg(DISTINCT array_to_string(path, '-'), ',') AS prj_path
	FROM gradle.build_tree
	GROUP BY bld_id, array_to_string(bld_path, '-')
)