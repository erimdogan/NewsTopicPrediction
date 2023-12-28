# -*- coding: utf-8 -*-
import pandas as pd
import numpy as np
from sqlalchemy import create_engine
from sklearn.feature_extraction.text import CountVectorizer
import random
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import MultinomialNB
import time
import nltk

#databae url : 'postgresql://postgres:1234@localhost/news'
# Connect to the database
def connect_to_db(database_url):
    engine = create_engine(database_url)
    return engine

#engine = connect_to_db('postgresql://postgres:1234@localhost/news')

# Read the data from the database
def read_data_from_db():
    data = pd.read_sql('SELECT * FROM news',connect_to_db('postgresql://postgres:1234@localhost/news'))
    data = data.drop_duplicates(subset=['text'])
    return data

#data = pd.read_sql('SELECT * FROM news',engine)

# Modify the data to use
def modify_data():
    data = read_data_from_db()
    core_data = data[['topic', 'text']]
    return core_data

#core_data = data[['topic', 'text']]

# Remove the rows with empty text
def remove_empty_rows():
    core_data_copy = modify_data().copy()
    core_data = core_data_copy[core_data_copy["text"] != '']
    return core_data

#core_data_copy = core_data.copy()
#core_data = core_data_copy[core_data_copy['text'] != '']

# Creating new column to store the class id's to use them in the model
def create_topic_id():
    core_data = remove_empty_rows()
    core_data['topic_id'] = core_data['topic'].factorize()[0]
    core_data.reset_index(drop=True, inplace=True) # reset index
    return core_data

#core_data["topic_id"] = core_data["topic"].factorize()[0]

# Creating a dictionary to store the class id's and the class names
def create_class_id_dictionary():
    core_data = create_topic_id()
    topic_id_df = core_data[['topic_id', 'topic']].drop_duplicates().sort_values('topic_id')
    topic_id_dict = dict(topic_id_df[['topic_id', 'topic']].values)
    return topic_id_dict

#topic_id_df = core_data[['topic', 'topic_id']].drop_duplicates().sort_values('topic_id')
#id_to_topic = dict(topic_id_df[['topic_id', 'topic']].values)

# splitting the data into train and test
x = np.array(create_topic_id()['text'])
y = np.array(create_topic_id()['topic_id'])

m = CountVectorizer()
x = m.fit_transform(x)

x_train, x_test, y_train, y_test = train_test_split(x,y, test_size=0.2)

# Training the model
model = MultinomialNB()
model.fit(x_train, y_train)


# Creating random number to test
def create_random_number():
    core_data = create_topic_id()
    if(len(core_data) > 0):
        random_number = random.randint(0, len(core_data['text']) - 1)
        random_number = min(random_number, len(core_data) - 1)
        return random_number
    else:
        raise ValueError('Data is empty. No data available for random selection.')

#random_number = random.randint(0, len(core_data))

# Testing the model
def test_model(model):
    core_data = create_topic_id()
    random_number = create_random_number()
    print('random number: ', random_number)
    print('len of core_data: ', len(core_data))

    if(random_number > len(core_data)):
        print('random number is out of range')
        return None, None, None

    text = core_data['text'][random_number]
    actual_topic = core_data['topic'][random_number]
    predicted_topic = create_class_id_dictionary()[model.predict(m.transform([text]))[0]]

    return text,actual_topic,predicted_topic

#print(core_data["text"][random_number])
#print('actual topic: ',core_data["topic"][random_number])
#print('predicted topic: ',id_to_topic[model.predict(m.transform([core_data["text"][random_number]]))[0]])

text,topic,prediction = test_model(model)

# Add predictions to the data if it is wrong
def add_predictions_to_data(text,actual_topic,prediction):
    text = text
    print('text: ', text)
    actual_topic=actual_topic
    print('actual topic: ', actual_topic)
    prediction=prediction
    print('predicted topic: ', prediction)
    if actual_topic != prediction:
        print('Prediction is wrong')
        #new_data = create_topic_id()
        new_row = [[actual_topic, text]]
        new_data = pd.DataFrame(new_row, columns=['topic', 'text'])
        #new_data = pd.concat([new_data, pd.DataFrame(new_row, columns=['topic', 'text'])],ignore_index=True)
        new_data.to_sql('news', connect_to_db('postgresql://postgres:1234@localhost/news'), index=False, if_exists='append')
        print('Prediction added to the data')
        core_data = read_data_from_db()[['topic', 'text']]
    else:
        print('Prediction is correct')


add_predictions_to_data(text, topic, prediction)