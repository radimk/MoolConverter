create view gradle.publications
  as (select b.id, b.path, r.group_id, r.artifact_id, r.base_version
      from mool_dedup.blds b
        join mool.relcfg_to_bld rb on b.id = rb.bld_id
        join mool.relcfgs r on rb.relcfg_id = r.id)
