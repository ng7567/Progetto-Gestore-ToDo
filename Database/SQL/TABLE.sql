--File contenente le definizioni di tutte le tabelle e le viste della base di dati "todo"

CREATE TABLE public.users (
    user_id SERIAL PRIMARY KEY, -- Chiave artificiale surrogata
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_hash_length CHECK (length(password) = 60),
    CONSTRAINT check_username_format CHECK (
        length(username) >= 3 AND 
        length(username) <= 20 AND 
        username ~ '^[a-zA-Z0-9_.-]+$'
    )
);

CREATE TABLE public.boards (
    board_id SERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    user_id INTEGER NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_board_user FOREIGN KEY (user_id) 
        REFERENCES public.users(user_id) ON DELETE CASCADE
);

-- Definizione del tipo enumerativo
CREATE TYPE public.priority_level AS ENUM ('ALTA', 'MEDIA', 'BASSA');

CREATE TABLE public.todos (
    todo_id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    expiry_date TIMESTAMP,
    url_link TEXT,
    image_path TEXT,
    background_color VARCHAR(7) DEFAULT '#FFFFFF',
    is_completed BOOLEAN DEFAULT FALSE,
    position_order INTEGER DEFAULT 1 NOT NULL,
    priority public.priority_level DEFAULT 'BASSA',
    board_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_todo_board FOREIGN KEY (board_id) 
        REFERENCES public.boards(board_id) ON DELETE CASCADE,
    CONSTRAINT check_expiry_after_created 
        CHECK (expiry_date >= (created_at - interval '1 minute') OR expiry_date IS NULL)
);

CREATE TABLE public.shared_todos (
    todo_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    position_order INTEGER DEFAULT 1 NOT NULL,
    PRIMARY KEY (todo_id, user_id), -- Chiave composta
    CONSTRAINT fk_shared_todo FOREIGN KEY (todo_id) 
        REFERENCES public.todos(todo_id) ON DELETE CASCADE,
    CONSTRAINT fk_shared_user FOREIGN KEY (user_id) 
        REFERENCES public.users(user_id) ON DELETE CASCADE
);

CREATE VIEW public.v_user_visible_todos AS
 SELECT t.todo_id,
    t.title,
    t.description,
    t.expiry_date,
    t.is_completed,
    t.priority,
    t.board_id,
    t.position_order,
    t.background_color,
    t.image_path,
    t.url_link,
    b.title AS board_title,
    u.user_id AS viewer_id,
    u.username AS owner_username,
    'OWNER'::text AS role
   FROM ((public.todos t
     JOIN public.boards b ON ((t.board_id = b.board_id)))
     JOIN public.users u ON ((b.user_id = u.user_id)))
UNION ALL
 SELECT t.todo_id,
    t.title,
    t.description,
    t.expiry_date,
    t.is_completed,
    t.priority,
    b_shared.board_id,
    st.position_order,
    t.background_color,
    t.image_path,
    t.url_link,
    b_shared.title AS board_title,
    st.user_id AS viewer_id,
    u_owner.username AS owner_username,
    'SHARED'::text AS role
   FROM ((((public.todos t
     JOIN public.shared_todos st ON ((t.todo_id = st.todo_id)))
     JOIN public.boards b_owner ON ((t.board_id = b_owner.board_id)))
     JOIN public.users u_owner ON ((b_owner.user_id = u_owner.user_id)))
     JOIN public.boards b_shared ON (((b_shared.user_id = st.user_id) AND ((b_shared.title)::text = (b_owner.title)::text))));