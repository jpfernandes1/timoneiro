user = {user_id, name, email, password, phone, role, created_at}

boat = {boat_id, name, description, type, capacity, price_per_hour, location, photo_url, *owner_id*}

booking = {booking_id, start_date, end_date, status, total_price, *user_id*, *boat_id*}

review = {review_id, rating, comment, date, *user_id*, *boat_id*}

payment = {payment_id, amount, status, payment_date, *booking_id*}

message = {message_id, content, sent_at, *booking_id*, *sender_id*}

boat_availability = {availability_id, start_date, end_date, *boat_id*}


obs.: The '*' indicates foreign keys
