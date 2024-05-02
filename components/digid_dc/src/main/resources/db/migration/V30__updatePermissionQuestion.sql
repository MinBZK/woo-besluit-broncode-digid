UPDATE services SET permission_question = name WHERE permission_question IS NULL OR permission_question = '';
