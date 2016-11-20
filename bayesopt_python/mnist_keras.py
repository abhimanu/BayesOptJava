import numpy as np
import time
from keras.datasets import mnist
from keras.models import Sequential
from keras.layers.core import Dense, Dropout, Activation
from keras.optimizers import Adam
from keras.utils import np_utils
from keras.regularizers import l1l2, l2, activity_l2
import os
#os.environ['MKL_NUM_THREADS'] = '7'
#os.environ['GOTO_NUM_THREADS'] = '7'
#os.environ['OMP_NUM_THREADS'] = '7'
#os.environ['THEANO_FLAGS'] = 'floatX=float32,openmp=True,device=cpu,blas.ldflags=-lblas -lgfortran'

def build_model(params):

    n_hidden_layers = int(np.round(params['n_hidden_layers'][ 0 ]))
    n_neurons = int(np.round(params['n_neurons'][ 0 ]))
    log_l1_weight_reg = np.float32(params['log_l1_weight_reg'][ 0 ])
    log_l2_weight_reg = np.float32(params['log_l2_weight_reg'][ 0 ])
    #prob_drop_out = float(params['prob_drop_out'][ 0 ].astype('float32'))
    prob_drop_out = np.float32(params['prob_drop_out'][ 0 ])
    log_l_rate = np.float32(params['log_learning_rate'][ 0 ])

    print  n_hidden_layers
    print n_neurons
    print  log_l1_weight_reg
    print  log_l2_weight_reg
    print   prob_drop_out
    print  log_l_rate

    model = Sequential()
    model.add(Dense(n_neurons, input_shape = (784,), W_regularizer=l1l2(l1 = np.exp(log_l1_weight_reg), \
        l2 = np.exp(log_l2_weight_reg))))
    model.add(Activation('relu'))    
    prob_drop_out = float(prob_drop_out)
    model.add(Dropout(prob_drop_out))
    #model.add(Dropout(0.35))
    for i in range(n_hidden_layers - 1):
        model.add(Dense(n_neurons, W_regularizer=l1l2(l1 = np.exp(log_l1_weight_reg), \
            l2 = np.exp(log_l2_weight_reg))))
        model.add(Activation('relu'))       
        model.add(Dropout(prob_drop_out))
        #model.add(Dropout(0.35))
    n_classes = 10
    model.add(Dense(n_classes))
    model.add(Activation('softmax'))    
    adam = Adam(lr=np.exp(log_l_rate), beta_1=0.9, beta_2=0.999, epsilon=1e-08)
    model.compile(loss='categorical_crossentropy', optimizer=adam, metrics=['accuracy'])

    return model

def evaluate_error_model(X_train, Y_train, X_test, Y_test, X_val, Y_val, params):

    #nb_epoch = 150
    nb_epoch = 50
    batch_size = 4000
    model = build_model(params)
    model.fit(X_train, Y_train, batch_size=batch_size, nb_epoch=nb_epoch, show_accuracy=True, verbose=2, \
        validation_data=(X_val, Y_val))
    loss, score = model.evaluate(X_val, Y_val, show_accuracy=True, verbose=0)
    #print('Val error:', 1.0 - score)
#    if score >= 1:
#        score = 0.999999
    #print('Val error:', score)
    print('Val error:', 1.0 - score)
    #return score #np.log((1 - score) / score)
    return np.log((1 - score) / score)

def evaluate_time_model(X_train, Y_train, X_test, Y_test, X_val, Y_val, params):

    nb_epoch = 1
    batch_size = 500
    model = build_model(params)
    start = time.time()
    for i in range(100):
        predictions = model.predict_classes(X_val, batch_size=X_val.shape[ 0 ], verbose=0)
    end = time.time()
    print('Avg. Prediction Time:', (end - start) / 100.0)
    return (end - start) / 100.0

#def main(n_neurons, callerObject=None):

def main(job_id, params_in, callerObject=None):
    print "params_in: ",params_in
    params={}
    params['n_hidden_layers']={}
    #params['n_hidden_layers'][ 0 ]=1;
    params['n_hidden_layers'][ 0 ]=params_in['n_hidden_layers'][ 0 ];
    params['n_neurons']={}
    #params['n_neurons'][ 0 ]=n_neurons
    params['n_neurons'][ 0 ]=params_in['n_neurons'][0]
    #params['n_neurons'][ 0 ]= 10 #300
    params['log_l1_weight_reg']= {}
    params['log_l1_weight_reg'][ 0 ] = -5
    params['log_l2_weight_reg']= {}
    params['log_l2_weight_reg'][ 0 ] = -5
    params['prob_drop_out'] = {}
    #params['prob_drop_out'][ 0 ] = '0.30'
    params['prob_drop_out'][ 0 ] = params_in['prob_drop_out'][ 0 ]
    params['log_learning_rate'] = {}
    params['log_learning_rate'][ 0 ] = -2


    nb_classes = 10
    (X_train, y_train), (X_test, y_test) = mnist.load_data()
    X_train = X_train.reshape(60000, 784)
    X_test = X_test.reshape(10000, 784)
    X_train = X_train.astype('float32')
    X_test = X_test.astype('float32')
    X_train /= 255
    X_test /= 255

    state = np.random.get_state()
    np.random.seed(0)
    suffle = np.random.permutation(60000)
    i_train = suffle[ 0 : 50000 ]
    i_val = suffle[ 50000 : 60000 ]
    np.random.set_state(state)
    X_val = X_train[ i_val, : ]
    y_val = y_train[ i_val ]
    X_train = X_train[ i_train, : ]
    y_train = y_train[ i_train ]

    Y_train = np_utils.to_categorical(y_train, nb_classes)
    Y_test = np_utils.to_categorical(y_test, nb_classes)
    Y_val = np_utils.to_categorical(y_val, nb_classes)

    evaluation = dict()
    #evaluation['error_task'] = evaluate_error_model(X_train, Y_train, X_test, Y_test, X_val, Y_val, params)
    error_task = evaluate_error_model(X_train, Y_train, X_test, Y_test, X_val, Y_val, params)
    #evaluation['time_task'] = evaluate_time_model(X_train, Y_train, X_test, Y_test, X_val, Y_val, params)

    if callerObject:
        callerObject.setObjective(float(error_task));
    return error_task #evaluation
