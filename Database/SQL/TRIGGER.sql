--File contenente tutti i trigger e le funzioni che ritornano trigger nella base di dati "todo"

-- Definisce la funzione per la creazione delle bacheche di default
CREATE FUNCTION public.create_default_boards() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Inserisce la bacheca dedicata agli esami e ai progetti
    INSERT INTO boards (title, description, user_id) 
    VALUES ('Universit' || U&'\00E0', 'Gestione dei corsi, esami da sostenere e scadenze dei progetti universitari.', NEW.user_id);
    
    -- Inserisce la bacheca dedicata alle attivita professionali
    INSERT INTO boards (title, description, user_id) 
    VALUES ('Lavoro', 'Task professionali, scadenze ufficio e obiettivi di carriera.', NEW.user_id);
    
    -- Inserisce la bacheca dedicata agli hobby e alla vita privata
    INSERT INTO boards (title, description, user_id) 
    VALUES ('Tempo liber' || U&'\00E0', 'Hobby, sport, viaggi e attivit' || U&'\00E0' || ' personali da svolgere nel tempo libero.', NEW.user_id);
    
    RETURN NEW;
END;
$$;
-- Associa la funzione alla tabella users dopo l'inserimento di un record
CREATE TRIGGER trigger_add_default_boards 
AFTER INSERT ON public.users 
FOR EACH ROW 
EXECUTE FUNCTION public.create_default_boards();


-- Definisce la funzione per il calcolo automatico della posizione del task
CREATE FUNCTION public.check_todo_position_logic() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_max_pos INTEGER;
    v_owner_id INTEGER;
BEGIN
    -- Recupera l'identificativo del proprietario della bacheca di destinazione
    SELECT user_id INTO v_owner_id FROM public.boards WHERE board_id = NEW.board_id;

    -- Determina la posizione massima attuale interrogando la vista unificata
    SELECT COALESCE(MAX(position_order), 0) INTO v_max_pos
    FROM public.v_user_visible_todos
    WHERE board_id = NEW.board_id AND viewer_id = v_owner_id;

    -- Assegna al nuovo task la posizione successiva all'ultima disponibile
    NEW.position_order := v_max_pos + 1;

    RETURN NEW;
END;
$$;
-- Associa la funzione alla tabella todos prima di ogni operazione di inserimento
CREATE TRIGGER trg_fix_todo_position_insert 
BEFORE INSERT ON public.todos 
FOR EACH ROW 
EXECUTE FUNCTION public.check_todo_position_logic();


-- Definisce la funzione per il posizionamento prioritario dei todo condivisi
CREATE FUNCTION public.check_shared_todo_position_logic() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_target_board_title VARCHAR(100);
    v_target_board_id INTEGER;
BEGIN
    -- Recupera il titolo della bacheca di origine del todo
    SELECT b.title INTO v_target_board_title
    FROM public.todos t
    JOIN public.boards b ON t.board_id = b.board_id
    WHERE t.todo_id = NEW.todo_id;

    -- Individua l'ID della bacheca omologa per l'utente ricevente
    SELECT board_id INTO v_target_board_id
    FROM public.boards
    WHERE title = v_target_board_title AND user_id = NEW.user_id;

    -- Verifica l'esistenza della bacheca prima di procedere allo scorrimento
    IF v_target_board_id IS NOT NULL THEN
        -- Incrementa la posizione dei todo di cui l'utente e titolare
        UPDATE public.todos
        SET position_order = position_order + 1
        WHERE board_id = v_target_board_id;

        -- Incrementa la posizione degli altri todo gia condivisi nella medesima bacheca
        UPDATE public.shared_todos st
        SET position_order = st.position_order + 1
        FROM public.todos t
        JOIN public.boards b ON t.board_id = b.board_id
        WHERE st.todo_id = t.todo_id
          AND st.user_id = NEW.user_id
          AND b.title = v_target_board_title;
    END IF;

    -- Assegna la prima posizione al todo appena condiviso
    NEW.position_order := 1;

    -- Restituisce il record pronto per la memorizzazione
    RETURN NEW;
END;
$$;
-- Associa la funzione alla tabella delle condivisioni prima dell'inserimento
CREATE TRIGGER trg_fix_shared_position_insert 
BEFORE INSERT ON public.shared_todos 
FOR EACH ROW 
EXECUTE FUNCTION public.check_shared_todo_position_logic();


-- Definisce la funzione per garantire l'esistenza della bacheca di destinazione
CREATE FUNCTION public.check_and_create_board_on_share() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    origin_board_title VARCHAR(100);
    target_has_board BOOLEAN;
BEGIN
    -- Individua il titolo della bacheca originale del task oggetto di condivisione
    SELECT b.title INTO origin_board_title
    FROM todos t JOIN boards b ON t.board_id = b.board_id
    WHERE t.todo_id = NEW.todo_id;

    -- Verifica se il destinatario possiede gia un contenitore con lo stesso titolo
    SELECT EXISTS (
        SELECT 1 FROM boards 
        WHERE user_id = NEW.user_id AND title = origin_board_title
    ) INTO target_has_board;

    -- Provvede alla creazione della bacheca qualora non risulti esistente
    IF NOT target_has_board THEN
        INSERT INTO boards (title, user_id) VALUES (origin_board_title, NEW.user_id);
    END IF;

    RETURN NEW;
END;
$$;
-- Associa la funzione alla tabella shared_todos per intervenire dopo ogni condivisione
CREATE TRIGGER trigger_ensure_board_exists 
AFTER INSERT ON public.shared_todos 
FOR EACH ROW 
EXECUTE FUNCTION public.check_and_create_board_on_share();


-- Definisce la funzione per la sincronizzazione delle bacheche durante lo spostamento
CREATE FUNCTION public.check_and_create_board_on_move() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    new_board_title VARCHAR(100);
    collaborator_record RECORD;
BEGIN
    -- Interrompe l'esecuzione se l'identificativo della bacheca non e variato
    IF NEW.board_id = OLD.board_id THEN RETURN NEW; END IF;

    -- Recupera il titolo della nuova bacheca di destinazione
    SELECT title INTO new_board_title FROM boards WHERE board_id = NEW.board_id;

    -- Itera su tutti i collaboratori associati al todo spostato
    FOR collaborator_record IN 
        SELECT user_id FROM shared_todos WHERE todo_id = NEW.todo_id
    LOOP
        -- Crea la bacheca omologa per il collaboratore qualora non esistente
        IF NOT EXISTS (
            SELECT 1 FROM boards 
            WHERE user_id = collaborator_record.user_id AND title = new_board_title
        ) THEN
            INSERT INTO boards (title, user_id) VALUES (new_board_title, collaborator_record.user_id);
        END IF;
    END LOOP;

    RETURN NEW;
END;
$$;
-- Attiva la funzione esclusivamente all'aggiornamento del campo board_id
CREATE TRIGGER trigger_ensure_board_on_move 
AFTER UPDATE OF board_id ON public.todos 
FOR EACH ROW 
EXECUTE FUNCTION public.check_and_create_board_on_move();



-- Definisce la funzione per la rimozione delle condivisioni associate a una bacheca
CREATE FUNCTION public.leave_shares_on_board_delete() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Rimuove la partecipazione ai task condivisi dell'utente
    -- per tutte le attivita associate a bacheche con lo stesso titolo
    -- di quella in fase di eliminazione.
    DELETE FROM shared_todos
    WHERE user_id = OLD.user_id 
    AND todo_id IN (
        SELECT t.todo_id
        FROM todos t
        JOIN boards b ON t.board_id = b.board_id
        WHERE b.title = OLD.title 
    );

    -- Restituisce il record bacheca per procedere alla rimozione fisica
    RETURN OLD;
END;
$$;
-- Associa la funzione alla tabella boards prima dell'operazione di cancellazione
CREATE TRIGGER trigger_leave_shares 
BEFORE DELETE ON public.boards 
FOR EACH ROW 
EXECUTE FUNCTION public.leave_shares_on_board_delete();



-- Definisce la funzione per impedire la ridenominazione di bacheche con task condivisi
CREATE FUNCTION public.prevent_collaborator_rename() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    shared_task_count INTEGER;
BEGIN
    -- Consente l'aggiornamento se il titolo non ha subito variazioni
    IF NEW.title = OLD.title THEN 
        RETURN NEW; 
    END IF;

    -- Conta i task condivisi con l'utente corrente che dipendono dal titolo attuale
    SELECT COUNT(*) INTO shared_task_count
    FROM shared_todos st
    JOIN todos t ON st.todo_id = t.todo_id
    JOIN boards owner_board ON t.board_id = owner_board.board_id
    WHERE st.user_id = NEW.user_id 
    AND owner_board.title = OLD.title;

    -- Impedisce la modifica se la bacheca funge da contenitore per task di altri utenti
    IF shared_task_count > 0 THEN
        RAISE EXCEPTION 'Impossibile rinominare: la bacheca contiene task condivisi. Solo il proprietario originale puo variare il titolo del progetto.';
    END IF;

    -- Restituisce il record per autorizzare l'aggiornamento (se non ci sono vincoli)
    RETURN NEW;
END;
$$;
-- Associa la funzione alla tabella boards prima della modifica del titolo
CREATE TRIGGER trigger_prevent_rename 
BEFORE UPDATE OF title ON public.boards 
FOR EACH ROW 
EXECUTE FUNCTION public.prevent_collaborator_rename();


-- Definisce la funzione per la creazione delle nuove bacheche a seguito di ridenominazione
CREATE OR REPLACE FUNCTION public.sync_board_title_on_rename() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    collaborator_id INT;
BEGIN
    -- Interrompe l'esecuzione se il titolo della bacheca non e variato
    IF NEW.title = OLD.title THEN 
        RETURN NEW; 
    END IF;

    -- Itera su tutti i collaboratori associati ai task della bacheca rinominata
    FOR collaborator_id IN
        SELECT DISTINCT st.user_id
        FROM todos t
        JOIN shared_todos st ON t.todo_id = st.todo_id
        WHERE t.board_id = NEW.board_id
    LOOP
        -- Inserisce la nuova bacheca per il collaboratore solo se non gia presente
        INSERT INTO boards (title, user_id)
        SELECT NEW.title, collaborator_id
        WHERE NOT EXISTS (
            SELECT 1 FROM boards 
            WHERE user_id = collaborator_id AND title = NEW.title
        );
    END LOOP;

    -- Restituisce il record bacheca aggiornato
    RETURN NEW;
END;
$$;
-- Associa la funzione alla tabella boards dopo l'aggiornamento del titolo
CREATE OR REPLACE TRIGGER trigger_sync_board_rename 
AFTER UPDATE OF title ON public.boards 
FOR EACH ROW 
EXECUTE FUNCTION public.sync_board_title_on_rename();



-- Definisce la funzione per l'aggiornamento del timestamp di modifica
CREATE FUNCTION public.update_timestamp() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Aggiorna l'attributo con la data e l'ora correnti
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;
-- Associa la funzione alla tabella boards
CREATE TRIGGER set_timestamp_boards 
BEFORE UPDATE ON public.boards 
FOR EACH ROW 
EXECUTE FUNCTION public.update_timestamp();
-- Associa la funzione alla tabella todos
CREATE TRIGGER set_timestamp_todos 
BEFORE UPDATE ON public.todos 
FOR EACH ROW 
EXECUTE FUNCTION public.update_timestamp();
-- Associa la funzione alla tabella users
CREATE TRIGGER set_timestamp_users 
BEFORE UPDATE ON public.users 
FOR EACH ROW 
EXECUTE FUNCTION public.update_timestamp();



