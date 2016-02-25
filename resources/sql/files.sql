-- name: get-raw-file*
SELECT id, rev, file, file_name, in_sheet_name, out_sheet_name
FROM files
WHERE id = :id;

-- name: valid-model*?
SELECT id
FROM files
WHERE id = :id;