--File contenente le definizioni di tutte le funzioni della base di dati "todo"

-- Esegue la ricerca testuale tramite l'ausilio della funzione format
CREATE OR REPLACE FUNCTION public.fn_search_todos(p_user_id INT, p_board_title VARCHAR, p_query TEXT)
RETURNS SETOF public.v_user_visible_todos AS $$
BEGIN
    -- Restituisce i record applicando un filtro case-insensitive sulla bacheca indicata
    RETURN QUERY
    SELECT * FROM v_user_visible_todos
    WHERE viewer_id = p_user_id
    AND board_title = p_board_title
    AND (title ILIKE format('%%%s%%', p_query) 
         OR description ILIKE format('%%%s%%', p_query));
END;
$$ LANGUAGE plpgsql;


-- Recupera i task in scadenza oggi per una specifica bacheca e utente
CREATE OR REPLACE FUNCTION public.fn_get_due_today(p_user_id INT, p_board_title VARCHAR)
RETURNS SETOF public.v_user_visible_todos AS $$
BEGIN
    -- Restituisce i record della vista filtrando per data corrente e titolo bacheca
    RETURN QUERY
    SELECT * FROM v_user_visible_todos
    WHERE viewer_id = p_user_id
    AND board_title = p_board_title
    AND expiry_date::DATE = CURRENT_DATE;
END;
$$ LANGUAGE plpgsql;


-- Recupera i todo che scadono entro una data specifica (limite superiore)
CREATE OR REPLACE FUNCTION public.fn_get_due_within(p_user_id INT, p_board_title VARCHAR, p_target_date DATE)
RETURNS SETOF public.v_user_visible_todos AS $$
BEGIN
    -- Seleziona i todo con scadenza compresa tra oggi e la data fornita in input
    RETURN QUERY
    SELECT * FROM v_user_visible_todos
    WHERE viewer_id = p_user_id
    AND board_title = p_board_title
    AND expiry_date::DATE >= CURRENT_DATE 
    AND expiry_date::DATE <= p_target_date;
END;
$$ LANGUAGE plpgsql;